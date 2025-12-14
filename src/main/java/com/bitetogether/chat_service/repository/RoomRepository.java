package com.bitetogether.chat_service.repository;

import com.bitetogether.chat_service.enums.RoomType;
import com.bitetogether.chat_service.model.Room;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface RoomRepository extends ReactiveMongoRepository<Room, String> {
  // Find all rooms where a user is a member
  Flux<Room> findByUserIdsContaining(Long userId);

  // Find all rooms where a user is a member, ordered by last message time
  Flux<Room> findByUserIdsContainingOrderByLastMessageAtDesc(Long userId);

  // Find rooms by type and containing a specific user
  Flux<Room> findByRoomTypeAndUserIdsContaining(RoomType roomType, Long userId);

  // Find rooms where a user is an admin
  Flux<Room> findByAdminIdsContaining(Long userId);
}
