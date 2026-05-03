package com.bitetogether.chat_service.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.chat_service.enums.conversation.ConversationType;
import com.bitetogether.chat_service.exception.KafkaEventProcessingException;
import com.bitetogether.chat_service.model.Conversation;
import com.bitetogether.chat_service.model.Participant;
import com.bitetogether.chat_service.repository.ConversationRepository;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ConversationEventListenerServiceTest {

  @Mock private ConversationRepository conversationRepository;
  @Mock private ParticipantRepository participantRepository;

  @InjectMocks private ConversationEventListenerService service;

  @Test
  void handleConversationEvents_WithValidEvent_CreatesDirectConversation() {
    // Arrange
    String payload =
        """
        {
          "user1Id": 1,
          "user2Id": 2,
          "eventTimestamp": "2026-04-09T11:30:00"
        }
        """;

    when(participantRepository.findByUserId(any(Long.class))).thenReturn(Flux.empty());

    Conversation savedConversation = new Conversation();
    savedConversation.setId("conv_1");
    savedConversation.setType(ConversationType.DIRECT);
    when(conversationRepository.save(any(Conversation.class)))
        .thenReturn(Mono.just(savedConversation));

    Participant participant = new Participant();
    participant.setId("p_1");
    when(participantRepository.save(any(Participant.class))).thenReturn(Mono.just(participant));

    // Act
    service.handleConversationEvents(payload, "conversation-events", 0, 10L);

    // Assert
    verify(conversationRepository).save(any(Conversation.class));
    verify(participantRepository, times(2)).save(any(Participant.class));
  }

  @Test
  void handleConversationEvents_WhenConversationExists_SkipsCreation() {
    // Arrange
    String payload =
        """
        {
          "user1Id": 1,
          "user2Id": 2,
          "eventTimestamp": "2026-04-09T11:30:00"
        }
        """;

    Participant existingP = new Participant();
    existingP.setConversationId("conv_existing");
    when(participantRepository.findByUserId(1L)).thenReturn(Flux.just(existingP));
    when(participantRepository.findByConversationIdAndUserId("conv_existing", 2L))
        .thenReturn(Mono.just(existingP));

    Conversation directConv = new Conversation();
    directConv.setId("conv_existing");
    directConv.setType(ConversationType.DIRECT);
    when(conversationRepository.findByIdIn(any())).thenReturn(Flux.just(directConv));

    // Act
    service.handleConversationEvents(payload, "conversation-events", 0, 10L);

    // Assert
    verify(conversationRepository, never()).save(any(Conversation.class));
  }

  @Test
  void handleConversationEvents_WithMalformedPayload_ThrowsKafkaException() {
    // Arrange
    String payload = "{ invalid }";

    // Act & Assert
    assertThrows(
        KafkaEventProcessingException.class,
        () -> service.handleConversationEvents(payload, "topic", 0, 0L));
  }
}
