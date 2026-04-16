package com.bitetogether.chat_service.event;

import com.bitetogether.chat_service.enums.bill.BillSessionRealtimeEvent;
import com.bitetogether.chat_service.enums.location.LocationUpdatedEvent;
import com.bitetogether.chat_service.enums.message.MessageCreatedEvent;
import com.bitetogether.chat_service.enums.message.MessageDeletedEvent;
import com.bitetogether.chat_service.enums.message.MessageUpdatedEvent;
import com.bitetogether.chat_service.enums.vote.VoteSessionRealtimeEvent;
import reactor.core.publisher.Flux;

public interface DomainEventPublisher {

  void publishMessageCreated(MessageCreatedEvent event);

  void publishMessageUpdated(MessageUpdatedEvent event);

  void publishMessageDeleted(MessageDeletedEvent event);

  void publishLocationUpdated(LocationUpdatedEvent event);

  void publishVoteSessionEvent(VoteSessionRealtimeEvent event);

  void publishBillSessionEvent(BillSessionRealtimeEvent event);

  Flux<MessageCreatedEvent> messageCreatedStream();

  Flux<MessageUpdatedEvent> messageUpdatedStream();

  Flux<MessageDeletedEvent> messageDeletedStream();

  Flux<LocationUpdatedEvent> locationUpdatedStream();

  Flux<VoteSessionRealtimeEvent> voteSessionStream();

  Flux<BillSessionRealtimeEvent> billSessionStream();
}
