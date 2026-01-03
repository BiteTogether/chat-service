package com.bitetogether.chat_service.repository;

import com.bitetogether.chat_service.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageRepository extends ReactiveMongoRepository<Message, String> {
  Flux<Message> findByRoomIdOrderByCreatedAtAsc(String roomId);

  Flux<Message> findByRoomIdOrderByCreatedAtAsc(String roomId, Pageable pageable);

  Flux<Message> findByReplyToMessageId(String replyToMessageId);

  // Pagination support methods
  Flux<Message> findByRoomId(String roomId, Pageable pageable);

  Mono<Long> countByRoomId(String roomId);
}
