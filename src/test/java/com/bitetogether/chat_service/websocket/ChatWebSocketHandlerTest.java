package com.bitetogether.chat_service.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.chat_service.configuration.websocket.WebSocketAuthService;
import com.bitetogether.chat_service.dto.location.LocationPayload;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import com.bitetogether.chat_service.service.LiveLocationService;
import com.bitetogether.chat_service.service.MessageService;
import com.bitetogether.chat_service.service.UserStateService;
import com.bitetogether.common.dto.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerTest {

  @Mock private RoomSessionRegistry registry;
  @Mock private MessageService messageService;
  @Mock private LiveLocationService liveLocationService;
  @Mock private WebSocketAuthService webSocketAuthService;
  @Mock private ParticipantRepository participantRepository;
  @Mock private UserStateService userStateService;
  @Mock private WebSocketSession session;
  @Mock private HandshakeInfo handshakeInfo;
  @Mock private WebSocketMessage incomingMessage;
  @Mock private WebSocketMessage outboundMessage;

  private ChatWebSocketHandler handler;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    handler =
        new ChatWebSocketHandler(
            registry,
            objectMapper,
            messageService,
            liveLocationService,
            webSocketAuthService,
            participantRepository,
            userStateService);

    when(session.getId()).thenReturn("ws-1");
    when(session.getHandshakeInfo()).thenReturn(handshakeInfo);
    when(handshakeInfo.getUri()).thenReturn(URI.create("ws://localhost/ws/chat"));
    when(handshakeInfo.getHeaders()).thenReturn(new HttpHeaders());
    when(webSocketAuthService.extractUserContext(any(), any()))
        .thenReturn(Optional.of(new UserContext(11L, "USER", "", "user_11")));
    when(participantRepository.findByUserId(11L)).thenReturn(Flux.empty());
    when(userStateService.markBackground(any())).thenReturn(Mono.empty());
    when(userStateService.markOffline(any())).thenReturn(Mono.empty());
  }

  @Test
  void handle_WithLocationUpdate_CallsLiveLocationService() throws Exception {
    String payload =
        objectMapper.writeValueAsString(
            java.util.Map.of(
                "conversationId",
                "conv_1",
                "action",
                "LOCATION_UPDATE",
                "location",
                new LocationPayload(10.77, 106.69, 10.0, 180.0, 1.0, Instant.now()),
                "isSharing",
                true));

    when(incomingMessage.getPayloadAsText()).thenReturn(payload);
    when(session.receive()).thenReturn(Flux.just(incomingMessage));
    when(liveLocationService.handleLocationUpdate(any(), any(), any(), any()))
        .thenReturn(Mono.empty());

    StepVerifier.create(handler.handle(session)).verifyComplete();

    verify(liveLocationService, times(1))
        .handleLocationUpdate(eq("conv_1"), eq(11L), any(LocationPayload.class), eq(true));
  }

  @Test
  void handle_WhenLocationUpdateFails_SendsError() throws Exception {
    String payload =
        objectMapper.writeValueAsString(
            java.util.Map.of(
                "conversationId",
                "conv_1",
                "action",
                "LOCATION_UPDATE",
                "location",
                new LocationPayload(10.77, 106.69, null, null, null, null),
                "isSharing",
                true));

    when(incomingMessage.getPayloadAsText()).thenReturn(payload);
    when(session.receive()).thenReturn(Flux.just(incomingMessage));
    when(session.textMessage(any())).thenReturn(outboundMessage);
    when(session.send(any())).thenReturn(Mono.empty());
    when(liveLocationService.handleLocationUpdate(any(), any(), any(), any()))
        .thenReturn(Mono.error(new RuntimeException("location failure")));

    StepVerifier.create(handler.handle(session)).verifyComplete();

    verify(session, times(1)).send(any());
  }
}
