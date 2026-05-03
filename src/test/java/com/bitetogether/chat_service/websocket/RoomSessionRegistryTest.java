package com.bitetogether.chat_service.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RoomSessionRegistryTest {

  @Mock private WebSocketSession session1;
  @Mock private WebSocketSession session2;

  private RoomSessionRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new RoomSessionRegistry();
  }

  @Test
  void join_AddsSessionToRoom() {
    // Arrange & Act
    registry.join("room_1", session1);

    // Assert
    StepVerifier.create(registry.getSessions("room_1").collectList())
        .assertNext(
            sessions -> {
              assertEquals(1, sessions.size());
              assertTrue(sessions.contains(session1));
            })
        .verifyComplete();
  }

  @Test
  void join_MultipleSessionsToSameRoom() {
    // Arrange & Act
    registry.join("room_1", session1);
    registry.join("room_1", session2);

    // Assert
    StepVerifier.create(registry.getSessions("room_1").collectList())
        .assertNext(sessions -> assertEquals(2, sessions.size()))
        .verifyComplete();
  }

  @Test
  void leave_RemovesSessionFromAllRooms() {
    // Arrange
    registry.join("room_1", session1);
    registry.join("room_2", session1);

    // Act
    registry.leave(session1);

    // Assert
    StepVerifier.create(registry.getSessions("room_1").collectList())
        .assertNext(sessions -> assertTrue(sessions.isEmpty()))
        .verifyComplete();

    StepVerifier.create(registry.getSessions("room_2").collectList())
        .assertNext(sessions -> assertTrue(sessions.isEmpty()))
        .verifyComplete();
  }

  @Test
  void leave_WithRoomId_RemovesFromSpecificRoom() {
    // Arrange
    registry.join("room_1", session1);
    registry.join("room_2", session1);

    // Act
    registry.leave("room_1", session1);

    // Assert
    StepVerifier.create(registry.getSessions("room_1").collectList())
        .assertNext(sessions -> assertTrue(sessions.isEmpty()))
        .verifyComplete();

    StepVerifier.create(registry.getSessions("room_2").collectList())
        .assertNext(sessions -> assertEquals(1, sessions.size()))
        .verifyComplete();
  }

  @Test
  void getSessions_ForNonExistentRoom_ReturnsEmpty() {
    // Act & Assert
    StepVerifier.create(registry.getSessions("nonexistent").collectList())
        .assertNext(sessions -> assertTrue(sessions.isEmpty()))
        .verifyComplete();
  }

  @Test
  void leave_FromNonExistentRoom_DoesNotThrow() {
    // Act & Assert - should not throw
    registry.leave("nonexistent", session1);
  }
}
