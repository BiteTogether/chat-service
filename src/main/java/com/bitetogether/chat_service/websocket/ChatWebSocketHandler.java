package com.bitetogether.chat_service.websocket;

import com.bitetogether.chat_service.configuration.websocket.WebSocketAuthService;
import com.bitetogether.chat_service.dto.location.LocationUpdateInbound;
import com.bitetogether.chat_service.dto.message.SendMessageInbound;
import com.bitetogether.chat_service.dto.websocket.WsEnvelopeDTO;
import com.bitetogether.chat_service.enums.websocket.WebSocketAction;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import com.bitetogether.chat_service.service.LiveLocationService;
import com.bitetogether.chat_service.service.MessageService;
import com.bitetogether.chat_service.service.UserStateService;
import com.bitetogether.common.dto.UserContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

  private final RoomSessionRegistry registry;
  private final ObjectMapper mapper;
  private final MessageService messageService;
  private final LiveLocationService liveLocationService;
  private final WebSocketAuthService webSocketAuthService;
  private final ParticipantRepository participantRepository;
  private final UserStateService userStateService;

  @Override
  @NonNull
  public Mono<Void> handle(@NonNull WebSocketSession session) {
    return Mono.fromCallable(() -> extractUserContext(session))
        .flatMap(
            userContext -> {
              Long userId = userContext.getUserId();
              log.info("WebSocket connected: sessionId={}, userId={}", session.getId(), userId);

              // Fire-and-forget: Set user state to BACKGROUND
              userStateService
                  .markBackground(userId)
                  .doOnError(
                      error ->
                          log.error(
                              "Failed to mark user {} as background: {}",
                              userId,
                              error.getMessage()))
                  .onErrorResume(e -> Mono.empty())
                  .subscribe();

              return subscribeToAllConversations(session, userId)
                  .then(
                      session
                          .receive()
                          .map(WebSocketMessage::getPayloadAsText)
                          .flatMap(
                              payload ->
                                  handlePayload(payload, session, userId)
                                      .onErrorResume(
                                          e -> {
                                            log.error(
                                                "Error processing message: {}", e.getMessage());
                                            return sendError(session, e.getMessage());
                                          }))
                          .doOnError(e -> log.error("WebSocket stream error: {}", e.getMessage()))
                          .onErrorResume(e -> Mono.empty())
                          .doFinally(
                              s -> {
                                log.info(
                                    "WebSocket session closed: sessionId={}, userId={}",
                                    session.getId(),
                                    userId);
                                registry.leave(session);

                                // Fire-and-forget: Mark user as offline
                                userStateService
                                    .markOffline(userId)
                                    .doOnError(
                                        error ->
                                            log.error(
                                                "Failed to mark user {} offline: {}",
                                                userId,
                                                error.getMessage()))
                                    .onErrorResume(e -> Mono.empty())
                                    .subscribe();
                              })
                          .then());
            })
        .onErrorResume(
            e -> {
              log.error("WebSocket authentication failed: {}", e.getMessage());
              return sendError(session, "Authentication failed: " + e.getMessage())
                  .then(session.close());
            });
  }

  private Mono<Void> subscribeToAllConversations(WebSocketSession session, Long userId) {
    return participantRepository
        .findByUserId(userId)
        .doOnNext(
            participant -> {
              log.info(
                  "Auto-subscribing session {} to conversation {}",
                  session.getId(),
                  participant.getConversationId());
              registry.join(participant.getConversationId(), session);
            })
        .then();
  }

  private UserContext extractUserContext(WebSocketSession session) {
    return webSocketAuthService
        .extractUserContext(
            session.getHandshakeInfo().getUri(), session.getHandshakeInfo().getHeaders())
        .orElseThrow(() -> new AccessDeniedException("User not authenticated"));
  }

  private Mono<Void> handlePayload(String payload, WebSocketSession session, Long userId) {
    return Mono.fromCallable(() -> mapper.readTree(payload))
        .flatMap(
            raw -> {
              WsEnvelopeDTO envelope = extractEnvelope(raw);
              return switch (envelope.action()) {
                case SUBSCRIBE -> {
                  log.info(
                      "Session {} subscribed to room {}",
                      session.getId(),
                      envelope.conversationId());
                  registry.join(envelope.conversationId(), session);
                  yield Mono.empty();
                }
                case UNSUBSCRIBE -> {
                  log.info(
                      "Session {} unsubscribed from room {}",
                      session.getId(),
                      envelope.conversationId());
                  registry.leave(envelope.conversationId(), session);
                  yield Mono.empty();
                }
                case SEND -> {
                  SendMessageInbound inbound = toSendMessageInbound(raw, envelope);
                  yield messageService
                      .processIncoming(inbound, userId)
                      .then() // Event published in service, RealtimeSubscriber handles broadcast
                      .onErrorResume(
                          e -> {
                            log.error("Failed to send message: {}", e.getMessage());
                            return sendError(session, "Failed to send message: " + e.getMessage());
                          });
                }
                case LOCATION_UPDATE -> {
                  LocationUpdateInbound inbound = toLocationUpdateInbound(raw, envelope);
                  yield liveLocationService
                      .handleLocationUpdate(
                          inbound.conversationId(), userId, inbound.location(), inbound.isSharing())
                      .onErrorResume(
                          e -> {
                            log.error("Failed to process location update: {}", e.getMessage());
                            return sendError(
                                session, "Failed to process location update: " + e.getMessage());
                          });
                }
                case VOTE_UPDATE, BILL_UPDATE -> Mono.empty();
                case TYPING -> handleTypingIndicator(envelope);
                case READ -> handleReadReceipt(envelope);
              };
            })
        .onErrorResume(
            e -> {
              log.error("Failed to parse incoming message: {}", e.getMessage());
              return sendError(session, "Invalid message format");
            });
  }

  private WsEnvelopeDTO extractEnvelope(JsonNode raw) {
    String conversationId = raw.path("conversationId").asText(null);
    String actionRaw = raw.path("action").asText(null);
    WebSocketAction action =
        actionRaw == null ? null : mapper.convertValue(actionRaw, WebSocketAction.class);
    return new WsEnvelopeDTO(conversationId, action);
  }

  private SendMessageInbound toSendMessageInbound(JsonNode raw, WsEnvelopeDTO envelope) {
    SendMessageInbound typed = mapper.convertValue(raw, SendMessageInbound.class);
    return new SendMessageInbound(
        envelope.conversationId(),
        envelope.action(),
        typed.messageType(),
        typed.content(),
        typed.postId(),
        typed.photoUrl());
  }

  private LocationUpdateInbound toLocationUpdateInbound(JsonNode raw, WsEnvelopeDTO envelope) {
    LocationUpdateInbound typed = mapper.convertValue(raw, LocationUpdateInbound.class);
    return new LocationUpdateInbound(
        envelope.conversationId(), envelope.action(), typed.location(), typed.isSharing());
  }

  private Mono<Void> handleTypingIndicator(WsEnvelopeDTO msg) {
    // Feature not yet implemented - typing indicator will be broadcasted via WebSocket in future
    log.debug("Typing indicator received for conversation: {}", msg.conversationId());
    return Mono.empty();
  }

  private Mono<Void> handleReadReceipt(WsEnvelopeDTO msg) {
    // Feature not yet implemented - read receipts will be handled via participant updates in future
    log.debug("Read receipt received for conversation: {}", msg.conversationId());
    return Mono.empty();
  }

  private Mono<Void> sendError(WebSocketSession session, String errorMessage) {
    try {
      String json =
          mapper.writeValueAsString(java.util.Map.of("type", "ERROR", "message", errorMessage));
      return session.send(Mono.just(session.textMessage(json)));
    } catch (JsonProcessingException e) {
      log.error("Failed to send error message: {}", e.getMessage());
      return Mono.empty();
    }
  }
}
