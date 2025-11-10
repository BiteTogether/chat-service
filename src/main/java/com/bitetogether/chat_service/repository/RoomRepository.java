package com.bitetogether.chat_service.repository;

import com.bitetogether.chat_service.model.Room;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface RoomRepository extends ReactiveMongoRepository<Room, String> {
  Flux<Room> findByUserIdsContaining(String userId);
}
