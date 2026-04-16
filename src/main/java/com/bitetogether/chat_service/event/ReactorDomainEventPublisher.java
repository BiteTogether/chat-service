package com.bitetogether.chat_service.event;

import com.bitetogether.chat_service.enums.bill.BillSessionRealtimeEvent;
import com.bitetogether.chat_service.enums.location.LocationUpdatedEvent;
import com.bitetogether.chat_service.enums.message.MessageCreatedEvent;
import com.bitetogether.chat_service.enums.message.MessageDeletedEvent;
import com.bitetogether.chat_service.enums.message.MessageUpdatedEvent;
import com.bitetogether.chat_service.enums.vote.VoteSessionRealtimeEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class ReactorDomainEventPublisher implements DomainEventPublisher {

  private final Sinks.Many<MessageCreatedEvent> createdSink =
      Sinks.many().multicast().onBackpressureBuffer();

  private final Sinks.Many<MessageUpdatedEvent> updatedSink =
      Sinks.many().multicast().onBackpressureBuffer();

  private final Sinks.Many<MessageDeletedEvent> deletedSink =
      Sinks.many().multicast().onBackpressureBuffer();

  private final Sinks.Many<LocationUpdatedEvent> locationUpdatedSink =
      Sinks.many().multicast().onBackpressureBuffer();

  private final Sinks.Many<VoteSessionRealtimeEvent> voteSessionSink =
      Sinks.many().multicast().onBackpressureBuffer();

  private final Sinks.Many<BillSessionRealtimeEvent> billSessionSink =
      Sinks.many().multicast().onBackpressureBuffer();

  @Override
  public void publishMessageCreated(MessageCreatedEvent event) {
    createdSink.tryEmitNext(event);
  }

  @Override
  public void publishMessageUpdated(MessageUpdatedEvent event) {
    updatedSink.tryEmitNext(event);
  }

  @Override
  public void publishMessageDeleted(MessageDeletedEvent event) {
    deletedSink.tryEmitNext(event);
  }

  @Override
  public void publishLocationUpdated(LocationUpdatedEvent event) {
    locationUpdatedSink.tryEmitNext(event);
  }

  @Override
  public void publishVoteSessionEvent(VoteSessionRealtimeEvent event) {
    voteSessionSink.tryEmitNext(event);
  }

  @Override
  public void publishBillSessionEvent(BillSessionRealtimeEvent event) {
    billSessionSink.tryEmitNext(event);
  }

  @Override
  public Flux<MessageCreatedEvent> messageCreatedStream() {
    return createdSink.asFlux();
  }

  @Override
  public Flux<MessageUpdatedEvent> messageUpdatedStream() {
    return updatedSink.asFlux();
  }

  @Override
  public Flux<MessageDeletedEvent> messageDeletedStream() {
    return deletedSink.asFlux();
  }

  @Override
  public Flux<LocationUpdatedEvent> locationUpdatedStream() {
    return locationUpdatedSink.asFlux();
  }

  @Override
  public Flux<VoteSessionRealtimeEvent> voteSessionStream() {
    return voteSessionSink.asFlux();
  }

  @Override
  public Flux<BillSessionRealtimeEvent> billSessionStream() {
    return billSessionSink.asFlux();
  }
}
