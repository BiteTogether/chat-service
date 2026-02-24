package com.bitetogether.chat_service.event;

import com.bitetogether.chat_service.enums.MessageCreatedEvent;
import com.bitetogether.chat_service.enums.MessageDeletedEvent;
import com.bitetogether.chat_service.enums.MessageUpdatedEvent;
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
}
