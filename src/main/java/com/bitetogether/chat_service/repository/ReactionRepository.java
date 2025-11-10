package com.bitetogether.chat_service.repository;

import com.bitetogether.chat_service.model.Reaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface ReactionRepository extends ReactiveMongoRepository<Reaction, String> {
  Flux<Reaction> findByMessageId(String messageId);
}
