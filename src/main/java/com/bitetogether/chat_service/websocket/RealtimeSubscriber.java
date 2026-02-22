package com.bitetogether.chat_service.websocket;

import com.bitetogether.chat_service.event.DomainEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeSubscriber {

  private final DomainEventPublisher publisher;
  private final RoomSessionRegistry registry;
  private final ObjectMapper mapper;

  @PostConstruct
  public void subscribe() {
    publisher
        .messageCreatedStream()
        .flatMap(event -> {
          log.info("Broadcasting message to conversation: {}", event.conversationId());
          return registry
              .getSessions(event.conversationId())
              .doOnNext(session -> log.debug("Sending to session: {}", session.getId()))
              .flatMap(session -> sendMessage(session, event.message()));
        })
        .subscribe(
            null,
            error -> log.error("Error in realtime subscriber: {}", error.getMessage()),
            () -> log.info("Realtime subscriber completed")
        );
  }

  private Mono<Void> sendMessage(WebSocketSession session, Object message) {
    try {
      String json = mapper.writeValueAsString(message);
      return session.send(Mono.just(session.textMessage(json)));
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize message: {}", e.getMessage());
      return Mono.empty();
    }
  }
}
