package com.bitetogether.chat_service.repository;

import com.bitetogether.chat_service.enums.ConversationType;
import com.bitetogether.chat_service.model.Conversation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface ConversationRepository extends ReactiveMongoRepository<Conversation, String> {

  Flux<Conversation> findByIdIn(java.util.Set<String> ids);

  Flux<Conversation> findByType(ConversationType type);
}
