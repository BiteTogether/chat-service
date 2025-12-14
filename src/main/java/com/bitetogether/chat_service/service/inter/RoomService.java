package com.bitetogether.chat_service.service.inter;

import com.bitetogether.chat_service.dto.request.RoomRequest;
import com.bitetogether.chat_service.dto.response.RoomResponse;
import com.bitetogether.common.dto.ApiResponse;
import java.time.LocalDateTime;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RoomService {
  // REST API methods (return ApiResponse wrapper)
  Mono<ApiResponse<RoomResponse>> createRoom(RoomRequest request);

  Mono<ApiResponse<RoomResponse>> getRoomById(String roomId);

  Flux<ApiResponse<RoomResponse>> getUserRooms(Long userId);

  Mono<ApiResponse<RoomResponse>> updateRoom(String roomId, RoomRequest request);

  Mono<ApiResponse<Void>> deleteRoom(String roomId);

  Mono<ApiResponse<RoomResponse>> addMembersToRoom(
      String roomId, List<Long> userIds, Long requesterId);

  Mono<ApiResponse<RoomResponse>> removeMemberFromRoom(
      String roomId, Long userId, Long requesterId);

  Mono<ApiResponse<RoomResponse>> promoteToAdmin(String roomId, Long userId, Long requesterId);

  Mono<ApiResponse<RoomResponse>> getOrCreateDirectRoom(Long userId1, Long userId2);

  // Direct methods for internal use (return raw data without wrapper)
  Mono<RoomResponse> createRoomDirect(RoomRequest request);

  Mono<RoomResponse> getRoomByIdDirect(String roomId);

  Flux<RoomResponse> getUserRoomsDirect(Long userId);

  Mono<RoomResponse> updateRoomDirect(String roomId, RoomRequest request);

  Mono<Void> deleteRoomDirect(String roomId);

  Mono<RoomResponse> addMembersToRoomDirect(String roomId, List<Long> userIds, Long requesterId);

  Mono<RoomResponse> removeMemberFromRoomDirect(String roomId, Long userId, Long requesterId);

  Mono<RoomResponse> promoteToAdminDirect(String roomId, Long userId, Long requesterId);

  Mono<RoomResponse> getOrCreateDirectRoomDirect(Long userId1, Long userId2);

  Mono<Void> updateLastMessage(String roomId, String messageId, LocalDateTime timestamp);
}
