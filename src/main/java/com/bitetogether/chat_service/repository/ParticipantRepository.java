package com.bitetogether.chat_service.repository;

import com.bitetogether.chat_service.enums.Role;
import com.bitetogether.chat_service.model.Participant;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ParticipantRepository extends ReactiveMongoRepository<Participant, String> {

  Mono<Boolean> existsByConversationIdAndUserId(String conversationId, Long userId);

  Flux<Participant> findByConversationId(String conversationId);

  Flux<Participant> findByUserId(Long userId);


  Mono<Participant> findByConversationIdAndUserId(String conversationId, Long userId);

  Mono<Void> deleteByConversationId(String conversationId);

  Mono<Void> deleteByConversationIdAndUserId(String conversationId, Long userId);

  Mono<Long> countByConversationIdAndRole(String conversationId, Role role);

  Flux<Participant> findByUserIdIn(java.util.Set<Long> userIds);
}
