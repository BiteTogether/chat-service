package com.bitetogether.chat_service.service;

import com.bitetogether.chat_service.dto.event.UserCreatedEvent;
import com.bitetogether.chat_service.dto.event.UserUpdatedEvent;
import com.bitetogether.chat_service.model.ChatUserSnapshot;
import com.bitetogether.chat_service.repository.ChatUserSnapshotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserEventListenerService {
  private final ChatUserSnapshotRepository chatUserSnapshotRepository;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @KafkaListener(
      topics = "#{@kafkaProperties.topic.userEvents}",
      groupId = "#{@kafkaProperties.consumer.groupId}",
      containerFactory = "kafkaListenerContainerFactory")
  public void handleUserEvents(
      @Payload String message,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset) {

    log.info(
        "Received message from topic: {}, partition: {}, offset: {}",
        topic,
        partition,
        offset);

    try {
      JsonNode jsonNode = objectMapper.readTree(message);

      if (jsonNode.has("userId")) {
        // Check if it's a create or update event based on version
        Long version = jsonNode.get("version").asLong();

        if (version == 0) {
          UserCreatedEvent event = objectMapper.treeToValue(jsonNode, UserCreatedEvent.class);
          handleUserCreatedEvent(event).subscribe();
        } else {
          UserUpdatedEvent event = objectMapper.treeToValue(jsonNode, UserUpdatedEvent.class);
          handleUserUpdatedEvent(event).subscribe();
        }
      }
    } catch (Exception e) {
      log.error("Error processing user event: {}", e.getMessage(), e);
    }
  }

  private Mono<ChatUserSnapshot> handleUserCreatedEvent(UserCreatedEvent event) {
    log.info("Processing UserCreatedEvent for userId: {}", event.getUserId());

    ChatUserSnapshot snapshot = new ChatUserSnapshot();
    snapshot.setUserId(event.getUserId());
    snapshot.setUsername(event.getUsername());
    snapshot.setFullName(event.getFullName());
    snapshot.setPhoneNumber(event.getPhoneNumber());
    snapshot.setAvatar(event.getAvatar());
    snapshot.setVersion(event.getVersion());
    snapshot.setLastSyncedAt(LocalDateTime.now());

    return chatUserSnapshotRepository
        .save(snapshot)
        .doOnSuccess(
            saved -> log.info("Successfully created ChatUserSnapshot for userId: {}", event.getUserId()))
        .doOnError(
            error -> log.error("Failed to create ChatUserSnapshot for userId: {}", event.getUserId(), error));
  }

  private Mono<ChatUserSnapshot> handleUserUpdatedEvent(UserUpdatedEvent event) {
    log.info("Processing UserUpdatedEvent for userId: {}", event.getUserId());

    return chatUserSnapshotRepository
        .findById(event.getUserId())
        .flatMap(
            existingSnapshot -> {
              // Only update if the event version is newer
              if (event.getVersion() > existingSnapshot.getVersion()) {
                existingSnapshot.setUsername(event.getUsername());
                existingSnapshot.setFullName(event.getFullName());
                existingSnapshot.setPhoneNumber(event.getPhoneNumber());
                existingSnapshot.setAvatar(event.getAvatar());
                existingSnapshot.setVersion(event.getVersion());
                existingSnapshot.setLastSyncedAt(LocalDateTime.now());

                return chatUserSnapshotRepository
                    .save(existingSnapshot)
                    .doOnSuccess(
                        saved ->
                            log.info(
                                "Successfully updated ChatUserSnapshot for userId: {} to version: {}",
                                event.getUserId(),
                                event.getVersion()))
                    .doOnError(
                        error ->
                            log.error(
                                "Failed to update ChatUserSnapshot for userId: {}",
                                event.getUserId(),
                                error));
              } else {
                log.info(
                    "Ignoring UserUpdatedEvent for userId: {} - event version {} is not newer than existing version {}",
                    event.getUserId(),
                    event.getVersion(),
                    existingSnapshot.getVersion());
                return Mono.just(existingSnapshot);
              }
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn(
                      "ChatUserSnapshot not found for userId: {}. Creating new snapshot.",
                      event.getUserId());
                  // Create new snapshot if not found
                  ChatUserSnapshot newSnapshot = new ChatUserSnapshot();
                  newSnapshot.setUserId(event.getUserId());
                  newSnapshot.setUsername(event.getUsername());
                  newSnapshot.setFullName(event.getFullName());
                  newSnapshot.setPhoneNumber(event.getPhoneNumber());
                  newSnapshot.setAvatar(event.getAvatar());
                  newSnapshot.setVersion(event.getVersion());
                  newSnapshot.setLastSyncedAt(LocalDateTime.now());

                  return chatUserSnapshotRepository.save(newSnapshot);
                }));
  }
}

