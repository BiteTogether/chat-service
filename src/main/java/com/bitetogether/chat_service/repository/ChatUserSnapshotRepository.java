package com.bitetogether.chat_service.repository;

import com.bitetogether.chat_service.model.ChatUserSnapshot;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatUserSnapshotRepository
    extends ReactiveMongoRepository<ChatUserSnapshot, Long> {
  // userId is now the primary key, so we can use findById(userId) directly
}
