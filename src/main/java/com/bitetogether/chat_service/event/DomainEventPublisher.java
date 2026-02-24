package com.bitetogether.chat_service.event;

import com.bitetogether.chat_service.enums.MessageCreatedEvent;
import com.bitetogether.chat_service.enums.MessageDeletedEvent;
import com.bitetogether.chat_service.enums.MessageUpdatedEvent;
import reactor.core.publisher.Flux;

public interface DomainEventPublisher {

  void publishMessageCreated(MessageCreatedEvent event);

  void publishMessageUpdated(MessageUpdatedEvent event);

  void publishMessageDeleted(MessageDeletedEvent event);

  Flux<MessageCreatedEvent> messageCreatedStream();

  Flux<MessageUpdatedEvent> messageUpdatedStream();

  Flux<MessageDeletedEvent> messageDeletedStream();
}
