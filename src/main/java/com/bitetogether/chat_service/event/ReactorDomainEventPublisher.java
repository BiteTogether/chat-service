package com.bitetogether.chat_service.event;

import com.bitetogether.chat_service.enums.MessageCreatedEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class ReactorDomainEventPublisher implements DomainEventPublisher {

  private final Sinks.Many<MessageCreatedEvent> sink =
      Sinks.many().multicast().onBackpressureBuffer();

  @Override
  public void publishMessageCreated(MessageCreatedEvent event) {
    sink.tryEmitNext(event);
  }

  @Override
  public Flux<MessageCreatedEvent> messageCreatedStream() {
    return sink.asFlux();
  }
}
