package com.bitetogether.chat_service.repository;

import com.bitetogether.chat_service.enums.ConversationType;
import com.bitetogether.chat_service.model.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Set;

public interface ConversationRepository extends ReactiveMongoRepository<Conversation, String> {

  Flux<Conversation> findByIdIn(Set<String> ids);

  Flux<Conversation> findByType(ConversationType type);

  Flux<Conversation> findByIdInOrderByLastMessageTimeDesc(Set<String> ids, Pageable pageable);

  Flux<Conversation> findByIdInAndLastMessageTimeLessThanOrderByLastMessageTimeDesc(
      Set<String> ids, LocalDateTime cursor, Pageable pageable);
}
