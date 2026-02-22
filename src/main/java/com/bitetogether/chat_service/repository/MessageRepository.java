package com.bitetogether.chat_service.repository;

import com.bitetogether.chat_service.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageRepository extends ReactiveMongoRepository<Message, String> {

  Mono<Long> countByConversationIdAndSequenceGreaterThan(String conversationId, Long sequence);

  Mono<Void> deleteByConversationId(String conversationId);

  Flux<Message> findByConversationIdOrderBySequenceDesc(String conversationId, Pageable pageable);

  Flux<Message> findByConversationIdAndSequenceLessThanOrderBySequenceDesc(
      String conversationId, Long sequence, Pageable pageable);

  Mono<Message> findTopByConversationIdOrderBySequenceDesc(String conversationId);
}
