package com.bitetogether.chat_service.repository;

import com.bitetogether.chat_service.model.Message;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface MessageRepository extends ReactiveMongoRepository<Message, String> {
  Flux<Message> findByRoomIdOrderByCreatedAtAsc(String roomId);
}
