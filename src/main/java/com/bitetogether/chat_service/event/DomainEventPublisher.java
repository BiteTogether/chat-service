package com.bitetogether.chat_service.event;

import com.bitetogether.chat_service.enums.MessageCreatedEvent;
import reactor.core.publisher.Flux;

public interface DomainEventPublisher {

  void publishMessageCreated(MessageCreatedEvent event);

  Flux<MessageCreatedEvent> messageCreatedStream();
}
