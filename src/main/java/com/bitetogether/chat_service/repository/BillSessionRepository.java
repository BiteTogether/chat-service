package com.bitetogether.chat_service.repository;

import com.bitetogether.chat_service.model.BillSession;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BillSessionRepository extends ReactiveMongoRepository<BillSession, String> {
  Flux<BillSession> findByConversationIdOrderByCreatedAtDesc(String conversationId);

  Mono<Void> deleteByConversationId(String conversationId);
}
