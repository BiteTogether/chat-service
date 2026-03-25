package com.bitetogether.chat_service.repository;

import com.bitetogether.chat_service.model.Conversation;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface ConversationRepository extends ReactiveMongoRepository<Conversation, String> {

  Flux<Conversation> findByIdIn(Set<String> ids);

  Flux<Conversation> findByIdInOrderByLastMessageTimeDesc(Set<String> ids, Pageable pageable);

  Flux<Conversation> findByIdInAndLastMessageTimeLessThanOrderByLastMessageTimeDesc(
      Set<String> ids, LocalDateTime cursor, Pageable pageable);
}
