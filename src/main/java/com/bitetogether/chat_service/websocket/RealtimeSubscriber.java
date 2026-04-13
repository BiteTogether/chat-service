package com.bitetogether.chat_service.websocket;

import com.bitetogether.chat_service.dto.message.ChatMessageDTO;
import com.bitetogether.chat_service.enums.WebSocketAction;
import com.bitetogether.chat_service.event.DomainEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeSubscriber {

  private final DomainEventPublisher publisher;
  private final RoomSessionRegistry registry;
  private final ObjectMapper mapper;

  @PostConstruct
  public void subscribe() {
    subscribeToMessageCreated();
    subscribeToMessageUpdated();
    subscribeToMessageDeleted();
    subscribeToLocationUpdated();
    subscribeToVoteUpdated();
    subscribeToBillUpdated();
  }

  private void subscribeToMessageCreated() {
    publisher
        .messageCreatedStream()
        .flatMap(
            event -> {
              log.info("Broadcasting new message to conversation: {}", event.conversationId());
              WebSocketOutboundMessage outbound =
                  WebSocketOutboundMessage.builder()
                      .action(WebSocketAction.SEND)
                      .conversationId(event.conversationId())
                      .message(event.message())
                      .build();
              return broadcastToConversation(event.conversationId(), outbound);
            })
        .subscribe(
            null,
            error -> log.error("Error in message created subscriber: {}", error.getMessage()),
            () -> log.info("Message created subscriber completed"));
  }

  private void subscribeToMessageUpdated() {
    publisher
        .messageUpdatedStream()
        .flatMap(
            event -> {
              log.info("Broadcasting updated message to conversation: {}", event.conversationId());
              WebSocketOutboundMessage outbound =
                  WebSocketOutboundMessage.builder()
                      .action(WebSocketAction.SEND)
                      .eventType("MESSAGE_UPDATED")
                      .conversationId(event.conversationId())
                      .message(event.message())
                      .build();
              return broadcastToConversation(event.conversationId(), outbound);
            })
        .subscribe(
            null,
            error -> log.error("Error in message updated subscriber: {}", error.getMessage()),
            () -> log.info("Message updated subscriber completed"));
  }

  private void subscribeToMessageDeleted() {
    publisher
        .messageDeletedStream()
        .flatMap(
            event -> {
              log.info("Broadcasting deleted message to conversation: {}", event.conversationId());
              WebSocketOutboundMessage outbound =
                  WebSocketOutboundMessage.builder()
                      .action(WebSocketAction.SEND)
                      .eventType("MESSAGE_DELETED")
                      .conversationId(event.conversationId())
                      .messageId(event.messageId())
                      .build();
              return broadcastToConversation(event.conversationId(), outbound);
            })
        .subscribe(
            null,
            error -> log.error("Error in message deleted subscriber: {}", error.getMessage()),
            () -> log.info("Message deleted subscriber completed"));
  }

  private void subscribeToLocationUpdated() {
    publisher
        .locationUpdatedStream()
        .flatMap(
            event -> {
              log.debug("Broadcasting location update to conversation: {}", event.conversationId());
              WebSocketOutboundMessage outbound =
                  WebSocketOutboundMessage.builder()
                      .action(WebSocketAction.LOCATION_UPDATE)
                      .eventType("LOCATION_UPDATED")
                      .conversationId(event.conversationId())
                      .location(event.location())
                      .build();
              return broadcastToConversation(event.conversationId(), outbound);
            })
        .subscribe(
            null,
            error -> log.error("Error in location updated subscriber: {}", error.getMessage()),
            () -> log.info("Location updated subscriber completed"));
  }

  private void subscribeToVoteUpdated() {
    publisher
        .voteSessionStream()
        .flatMap(
            event -> {
              log.debug("Broadcasting vote update to conversation: {}", event.conversationId());
              WebSocketOutboundMessage outbound =
                  WebSocketOutboundMessage.builder()
                      .action(WebSocketAction.VOTE_UPDATE)
                      .eventType(event.eventType())
                      .conversationId(event.conversationId())
                      .voteSession(event.voteSession())
                      .build();
              return broadcastToConversation(event.conversationId(), outbound);
            })
        .subscribe(
            null,
            error -> log.error("Error in vote updated subscriber: {}", error.getMessage()),
            () -> log.info("Vote updated subscriber completed"));
  }

  private void subscribeToBillUpdated() {
    publisher
        .billSessionStream()
        .flatMap(
            event -> {
              log.debug("Broadcasting bill update to conversation: {}", event.conversationId());
              WebSocketOutboundMessage outbound =
                  WebSocketOutboundMessage.builder()
                      .action(WebSocketAction.BILL_UPDATE)
                      .eventType(event.eventType())
                      .conversationId(event.conversationId())
                      .billSession(event.billSession())
                      .build();
              return broadcastToConversation(event.conversationId(), outbound);
            })
        .subscribe(
            null,
            error -> log.error("Error in bill updated subscriber: {}", error.getMessage()),
            () -> log.info("Bill updated subscriber completed"));
  }

  private Mono<Void> broadcastToConversation(String conversationId, Object message) {
    return registry
        .getSessions(conversationId)
        .doOnNext(session -> log.debug("Sending to session: {}", session.getId()))
        .flatMap(session -> sendMessage(session, message))
        .then();
  }

  private Mono<Void> sendMessage(WebSocketSession session, Object message) {
    try {
      String json = mapper.writeValueAsString(message);
      return session.send(Mono.just(session.textMessage(json)));
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize message: {}", e.getMessage());
      return Mono.empty();
    }
  }

  /** WebSocket outbound message wrapper for real-time events */
  @Data
  @Builder
  private static class WebSocketOutboundMessage {
    private WebSocketAction action;
    private String eventType;
    private String conversationId;
    private String messageId;
    private ChatMessageDTO message;
    private com.bitetogether.chat_service.dto.location.LiveLocationSnapshot location;
    private com.bitetogether.chat_service.dto.vote.VoteSessionDTO voteSession;
    private com.bitetogether.chat_service.dto.bill.BillSessionDTO billSession;
  }
}
