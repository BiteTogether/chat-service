package com.bitetogether.chat_service.event;

import com.bitetogether.chat_service.configuration.kafka.KafkaProperties;
import com.bitetogether.chat_service.dto.event.MessageNotificationEvent;
import com.bitetogether.chat_service.dto.event.MessageNotificationEvent.ChatObject;
import com.bitetogether.chat_service.dto.message.ChatMessageDTO;
import com.bitetogether.chat_service.enums.conversation.ConversationType;
import com.bitetogether.chat_service.model.Participant;
import com.bitetogether.chat_service.repository.ChatUserSnapshotRepository;
import com.bitetogether.chat_service.repository.ConversationRepository;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatNotificationProducer {

  private static final int CONTENT_PREVIEW_MAX_LENGTH = 100;

  private final DomainEventPublisher domainEventPublisher;
  private final ParticipantRepository participantRepository;
  private final ConversationRepository conversationRepository;
  private final ChatUserSnapshotRepository chatUserSnapshotRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final KafkaProperties kafkaProperties;

  @PostConstruct
  public void subscribe() {
    domainEventPublisher
        .messageCreatedStream()
        .flatMap(
            event ->
                buildAndPublish(event.conversationId(), event.message())
                    .onErrorResume(
                        ex -> {
                          log.error(
                              "Failed to publish notification for conversation {}: {}",
                              event.conversationId(),
                              ex.getMessage());
                          return Mono.empty();
                        }))
        .subscribe(
            null,
            error ->
                log.error("Chat notification producer stream terminated: {}", error.getMessage()),
            () -> log.info("Chat notification producer stream completed"));
  }

  private Mono<Void> buildAndPublish(String conversationId, ChatMessageDTO message) {
    return participantRepository
        .findByConversationId(conversationId)
        .filter(p -> !p.getUserId().equals(message.getSenderId()))
        .map(Participant::getUserId)
        .collectList()
        .filter(recipientIds -> !recipientIds.isEmpty())
        .flatMap(
            recipientIds ->
                conversationRepository
                    .findById(conversationId)
                    .flatMap(
                        conversation -> {
                          Mono<String> titleMono;
                          if (conversation.getType() == ConversationType.DIRECT) {
                            titleMono =
                                chatUserSnapshotRepository
                                    .findById(message.getSenderId())
                                    .map(snapshot -> "New message from " + snapshot.getUsername())
                                    .defaultIfEmpty("New message");
                          } else {
                            titleMono = Mono.just("New message in " + conversation.getName());
                          }

                          return titleMono.flatMap(
                              title -> {
                                ChatObject chatObject =
                                    ChatObject.builder()
                                        .id(conversationId)
                                        .name(conversation.getName())
                                        .recipientUserIds(recipientIds)
                                        .build();

                                String contentPreview = truncate(message.getContent());
                                LocalDateTime sendAt = LocalDateTime.now(ZoneOffset.UTC);

                                return Flux.fromIterable(recipientIds)
                                    .flatMap(
                                        recipientId -> {
                                          MessageNotificationEvent event =
                                              MessageNotificationEvent.builder()
                                                  .userId(recipientId)
                                                  .title(title)
                                                  .message(contentPreview)
                                                  .sendAt(sendAt)
                                                  .chatObject(chatObject)
                                                  .build();
                                          return publish(event, conversationId);
                                        })
                                    .then();
                              });
                        }));
  }

  private Mono<Void> publish(MessageNotificationEvent event, String conversationId) {
    String topic = kafkaProperties.getTopic().getChatNotificationEvents();
    log.info(
        "Publishing notification to topic [{}] for user {} in conversation {}",
        topic,
        event.getUserId(),
        conversationId);

    return Mono.fromFuture(kafkaTemplate.send(topic, conversationId, event).toCompletableFuture())
        .doOnSuccess(
            result ->
                log.debug(
                    "Notification published for user {} in conversation {}",
                    event.getUserId(),
                    conversationId))
        .then();
  }

  private String truncate(String content) {
    if (content == null) {
      return "";
    }
    return content.length() <= CONTENT_PREVIEW_MAX_LENGTH
        ? content
        : content.substring(0, CONTENT_PREVIEW_MAX_LENGTH) + "...";
  }
}
