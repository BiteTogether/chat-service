package com.bitetogether.chat_service.repository;

import com.bitetogether.chat_service.enums.RoomType;
import com.bitetogether.chat_service.model.Room;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface RoomRepository extends ReactiveMongoRepository<Room, String> {
  // Find all rooms where a user is a member
  Flux<Room> findByUserIdsContaining(String userId);

  // Find all rooms where a user is a member, ordered by last message time
  Flux<Room> findByUserIdsContainingOrderByLastMessageAtDesc(String userId);

  // Find rooms by type and containing a specific user
  Flux<Room> findByRoomTypeAndUserIdsContaining(RoomType roomType, String userId);

  // Find rooms where a user is an admin
  Flux<Room> findByAdminIdsContaining(String userId);
}
