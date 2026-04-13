package com.bitetogether.chat_service.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.chat_service.exception.KafkaEventProcessingException;
import com.bitetogether.chat_service.model.ChatUserSnapshot;
import com.bitetogether.chat_service.repository.ChatUserSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class UserEventListenerServiceTest {

  @Mock private ChatUserSnapshotRepository chatUserSnapshotRepository;

  @InjectMocks private UserEventListenerService userEventListenerService;

  @Test
  void handleUserEvents_WithDeletedEvent_DeletesSnapshot() {
    String payload =
        """
        {
          "userId": 11,
          "eventTimestamp": "2026-04-09T11:30:00",
          "version": 5
        }
        """;

    when(chatUserSnapshotRepository.deleteById(11L)).thenReturn(Mono.empty());

    userEventListenerService.handleUserEvents(payload, "user-events", 0, 10L);

    verify(chatUserSnapshotRepository, times(1)).deleteById(11L);
    verify(chatUserSnapshotRepository, never()).findById(any(Long.class));
    verify(chatUserSnapshotRepository, never()).save(any(ChatUserSnapshot.class));
  }

  @Test
  void handleUserEvents_WithCreatedEvent_SavesSnapshot() {
    String payload =
        """
        {
          "userId": 11,
          "username": "john_doe",
          "fullName": "John Doe",
          "phoneNumber": "1234567890",
          "avatar": "avatar-url",
          "eventTimestamp": "2026-04-09T11:30:00",
          "version": 0
        }
        """;

    when(chatUserSnapshotRepository.save(any(ChatUserSnapshot.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    userEventListenerService.handleUserEvents(payload, "user-events", 0, 10L);

    verify(chatUserSnapshotRepository, times(1)).save(any(ChatUserSnapshot.class));
    verify(chatUserSnapshotRepository, never()).deleteById(any(Long.class));
  }

  @Test
  void handleUserEvents_WithUpdatedEvent_UpdatesExistingSnapshot() {
    String payload =
        """
        {
          "userId": 11,
          "username": "john_doe_updated",
          "fullName": "John Doe Updated",
          "phoneNumber": "1234567890",
          "avatar": "avatar-url-updated",
          "eventTimestamp": "2026-04-09T11:30:00",
          "version": 2
        }
        """;

    ChatUserSnapshot existing = new ChatUserSnapshot();
    ReflectionTestUtils.setField(existing, "userId", 11L);
    ReflectionTestUtils.setField(existing, "version", 1L);

    when(chatUserSnapshotRepository.findById(11L)).thenReturn(Mono.just(existing));
    when(chatUserSnapshotRepository.save(any(ChatUserSnapshot.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    userEventListenerService.handleUserEvents(payload, "user-events", 0, 10L);

    verify(chatUserSnapshotRepository, times(1)).findById(11L);
    verify(chatUserSnapshotRepository, times(1)).save(any(ChatUserSnapshot.class));
    verify(chatUserSnapshotRepository, never()).deleteById(any(Long.class));
  }

  @Test
  void handleUserEvents_WithMalformedPayload_ThrowsKafkaEventProcessingException() {
    String payload = "{invalid-json}";

    assertThrows(
        KafkaEventProcessingException.class,
        () -> userEventListenerService.handleUserEvents(payload, "user-events", 0, 10L));

    verify(chatUserSnapshotRepository, never()).findById(any(Long.class));
    verify(chatUserSnapshotRepository, never()).save(any(ChatUserSnapshot.class));
    verify(chatUserSnapshotRepository, never()).deleteById(any(Long.class));
  }
}
