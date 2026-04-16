package com.bitetogether.chat_service.repository;

import com.bitetogether.chat_service.enums.Role;
import com.bitetogether.chat_service.model.Participant;
import java.util.Set;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ParticipantRepository extends ReactiveMongoRepository<Participant, String> {

  Mono<Boolean> existsByConversationIdAndUserId(String conversationId, Long userId);

  Flux<Participant> findByConversationId(String conversationId);

  Flux<Participant> findByUserId(Long userId);

  Flux<Participant> findByConversationIdInAndUserIdIn(
      Set<String> conversationIds, Set<Long> userIds);

  Mono<Participant> findByConversationIdAndUserId(String conversationId, Long userId);

  Mono<Void> deleteByConversationId(String conversationId);

  Mono<Void> deleteByConversationIdAndUserId(String conversationId, Long userId);

  Mono<Long> countByConversationIdAndRole(String conversationId, Role role);
}
