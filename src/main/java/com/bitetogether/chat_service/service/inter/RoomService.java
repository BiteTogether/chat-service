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

  Flux<ApiResponse<RoomResponse>> getUserRooms(String userId);

  Mono<ApiResponse<RoomResponse>> updateRoom(String roomId, RoomRequest request);

  Mono<ApiResponse<Void>> deleteRoom(String roomId);

  Mono<ApiResponse<RoomResponse>> addMembersToRoom(
      String roomId, List<String> userIds, String requesterId);

  Mono<ApiResponse<RoomResponse>> removeMemberFromRoom(
      String roomId, String userId, String requesterId);

  Mono<ApiResponse<RoomResponse>> promoteToAdmin(String roomId, String userId, String requesterId);

  Mono<ApiResponse<RoomResponse>> getOrCreateDirectRoom(String userId1, String userId2);

  // Direct methods for internal use (return raw data without wrapper)
  Mono<RoomResponse> createRoomDirect(RoomRequest request);

  Mono<RoomResponse> getRoomByIdDirect(String roomId);

  Flux<RoomResponse> getUserRoomsDirect(String userId);

  Mono<RoomResponse> updateRoomDirect(String roomId, RoomRequest request);

  Mono<Void> deleteRoomDirect(String roomId);

  Mono<RoomResponse> addMembersToRoomDirect(
      String roomId, List<String> userIds, String requesterId);

  Mono<RoomResponse> removeMemberFromRoomDirect(String roomId, String userId, String requesterId);

  Mono<RoomResponse> promoteToAdminDirect(String roomId, String userId, String requesterId);

  Mono<RoomResponse> getOrCreateDirectRoomDirect(String userId1, String userId2);

  Mono<Void> updateLastMessage(String roomId, String messageId, LocalDateTime timestamp);
}
