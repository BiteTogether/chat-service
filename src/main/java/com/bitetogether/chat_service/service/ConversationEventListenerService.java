package com.bitetogether.chat_service.service;

import com.bitetogether.chat_service.dto.event.CreateConversationEvent;
import com.bitetogether.chat_service.enums.conversation.ConversationType;
import com.bitetogether.chat_service.enums.conversation.Role;
import com.bitetogether.chat_service.exception.KafkaEventProcessingException;
import com.bitetogether.chat_service.model.Conversation;
import com.bitetogether.chat_service.model.Participant;
import com.bitetogether.chat_service.repository.ConversationRepository;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationEventListenerService {
  private final ConversationRepository conversationRepository;
  private final ParticipantRepository participantRepository;

  @KafkaListener(
      topics = "#{@kafkaProperties.topic.conversationEvents}",
      groupId = "#{@kafkaProperties.consumer.groupId}",
      containerFactory = "kafkaListenerContainerFactory")
  public void handleConversationEvents(
      @Payload String message,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset) {

    log.info(
        "Received message from topic: {}, partition: {}, offset: {}", topic, partition, offset);

    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.registerModule(new JavaTimeModule());

      CreateConversationEvent event = mapper.readValue(message, CreateConversationEvent.class);
      handleCreateConversationEvent(event).block();
    } catch (Exception e) {
      log.error("Error processing conversation event: {}", e.getMessage(), e);
      // Re-throw to trigger the error handler and prevent offset commit
      throw new KafkaEventProcessingException("Failed to process conversation event", e);
    }
  }

  private Mono<Conversation> handleCreateConversationEvent(CreateConversationEvent event) {
    log.info(
        "Processing CreateConversationEvent for users: {} and {}",
        event.getUser1Id(),
        event.getUser2Id());

    Set<Long> participantIds = new HashSet<>();
    participantIds.add(event.getUser1Id());
    participantIds.add(event.getUser2Id());

    // Check if a direct conversation already exists between these two users
    return checkExistingDirectConversation(participantIds)
        .flatMap(
            exists -> {
              if (Boolean.TRUE.equals(exists)) {
                log.info(
                    "Direct conversation already exists between users {} and {}",
                    event.getUser1Id(),
                    event.getUser2Id());
                return Mono.empty();
              } else {
                return createDirectConversation(participantIds);
              }
            });
  }

  private Mono<Boolean> checkExistingDirectConversation(Set<Long> participantIds) {
    List<Long> userIds = participantIds.stream().toList();
    Long user1 = userIds.get(0);
    Long user2 = userIds.get(1);

    // Find conversations where both users are participants
    return participantRepository
        .findByUserId(user1)
        .flatMap(
            p1 ->
                participantRepository
                    .findByConversationIdAndUserId(p1.getConversationId(), user2)
                    .map(p2 -> p1.getConversationId()))
        .collectList()
        .flatMap(
            conversationIds -> {
              if (conversationIds.isEmpty()) {
                return Mono.just(false);
              }
              // Check if any of these are DIRECT conversations
              return conversationRepository
                  .findByIdIn(new HashSet<>(conversationIds))
                  .filter(c -> c.getType() == ConversationType.DIRECT)
                  .hasElements();
            });
  }

  private Mono<Conversation> createDirectConversation(Set<Long> participantIds) {
    Conversation conversation = new Conversation();
    conversation.setType(ConversationType.DIRECT);
    // For DIRECT conversations, name can be null (handled on client side)
    conversation.setName(null);
    conversation.setAvatarUrl(null);

    return conversationRepository
        .save(conversation)
        .flatMap(
            savedConversation ->
                createParticipants(savedConversation.getId(), participantIds)
                    .then(Mono.just(savedConversation)))
        .doOnSuccess(
            saved ->
                log.info("Successfully created direct conversation with ID: {}", saved.getId()))
        .doOnError(
            error ->
                log.error(
                    "Failed to create direct conversation for users: {}", participantIds, error));
  }

  private Flux<Participant> createParticipants(String conversationId, Set<Long> userIds) {
    return Flux.fromIterable(userIds)
        .map(
            userId -> {
              Participant participant = new Participant();
              participant.setConversationId(conversationId);
              participant.setUserId(userId);
              participant.setRole(Role.MEMBER); // Both are members in direct conversation
              participant.setLastReadMessageSequence(0L);
              return participant;
            })
        .flatMap(participantRepository::save);
  }
}
