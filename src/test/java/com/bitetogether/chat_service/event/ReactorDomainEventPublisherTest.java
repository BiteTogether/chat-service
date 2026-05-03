package com.bitetogether.chat_service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bitetogether.chat_service.dto.message.ChatMessageDTO;
import com.bitetogether.chat_service.enums.bill.BillSessionRealtimeEvent;
import com.bitetogether.chat_service.enums.location.LocationUpdatedEvent;
import com.bitetogether.chat_service.enums.message.MessageCreatedEvent;
import com.bitetogether.chat_service.enums.message.MessageDeletedEvent;
import com.bitetogether.chat_service.enums.message.MessageUpdatedEvent;
import com.bitetogether.chat_service.enums.vote.VoteSessionRealtimeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class ReactorDomainEventPublisherTest {

  private ReactorDomainEventPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new ReactorDomainEventPublisher();
  }

  @Test
  void publishMessageCreated_EmitsOnStream() {
    // Arrange
    ChatMessageDTO dto = new ChatMessageDTO();
    dto.setId("msg_1");
    MessageCreatedEvent event = new MessageCreatedEvent("conv_1", dto);

    // Act & Assert
    StepVerifier.create(publisher.messageCreatedStream().take(1))
        .then(() -> publisher.publishMessageCreated(event))
        .assertNext(
            e -> {
              assertEquals("conv_1", e.conversationId());
              assertEquals("msg_1", e.message().getId());
            })
        .verifyComplete();
  }

  @Test
  void publishMessageUpdated_EmitsOnStream() {
    // Arrange
    ChatMessageDTO dto = new ChatMessageDTO();
    dto.setId("msg_1");
    MessageUpdatedEvent event = new MessageUpdatedEvent("conv_1", dto);

    // Act & Assert
    StepVerifier.create(publisher.messageUpdatedStream().take(1))
        .then(() -> publisher.publishMessageUpdated(event))
        .assertNext(e -> assertEquals("conv_1", e.conversationId()))
        .verifyComplete();
  }

  @Test
  void publishMessageDeleted_EmitsOnStream() {
    // Arrange
    MessageDeletedEvent event = new MessageDeletedEvent("conv_1", "msg_1");

    // Act & Assert
    StepVerifier.create(publisher.messageDeletedStream().take(1))
        .then(() -> publisher.publishMessageDeleted(event))
        .assertNext(
            e -> {
              assertEquals("conv_1", e.conversationId());
              assertEquals("msg_1", e.messageId());
            })
        .verifyComplete();
  }

  @Test
  void publishLocationUpdated_EmitsOnStream() {
    // Arrange
    LocationUpdatedEvent event = new LocationUpdatedEvent("conv_1", null);

    // Act & Assert
    StepVerifier.create(publisher.locationUpdatedStream().take(1))
        .then(() -> publisher.publishLocationUpdated(event))
        .assertNext(e -> assertEquals("conv_1", e.conversationId()))
        .verifyComplete();
  }

  @Test
  void publishVoteSessionEvent_EmitsOnStream() {
    // Arrange
    VoteSessionRealtimeEvent event = new VoteSessionRealtimeEvent("conv_1", "VOTE_CREATED", null);

    // Act & Assert
    StepVerifier.create(publisher.voteSessionStream().take(1))
        .then(() -> publisher.publishVoteSessionEvent(event))
        .assertNext(e -> assertEquals("VOTE_CREATED", e.eventType()))
        .verifyComplete();
  }

  @Test
  void publishBillSessionEvent_EmitsOnStream() {
    // Arrange
    BillSessionRealtimeEvent event = new BillSessionRealtimeEvent("conv_1", "BILL_CREATED", null);

    // Act & Assert
    StepVerifier.create(publisher.billSessionStream().take(1))
        .then(() -> publisher.publishBillSessionEvent(event))
        .assertNext(e -> assertEquals("BILL_CREATED", e.eventType()))
        .verifyComplete();
  }
}
