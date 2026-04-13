package com.bitetogether.chat_service.repository;

import com.bitetogether.chat_service.model.VoteSession;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VoteSessionRepository extends ReactiveMongoRepository<VoteSession, String> {
  Flux<VoteSession> findByConversationIdOrderByCreatedAtDesc(String conversationId);

  Mono<Void> deleteByConversationId(String conversationId);
}
