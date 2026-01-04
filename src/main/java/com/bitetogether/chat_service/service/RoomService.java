package com.bitetogether.chat_service.service;

import com.bitetogether.chat_service.dto.request.RoomRequest;
import com.bitetogether.chat_service.dto.response.RoomResponse;
import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.dto.ApiResponsePaginationDTO;
import java.time.LocalDateTime;
import java.util.List;
import reactor.core.publisher.Mono;

public interface RoomService {
  Mono<ApiResponseDTO<RoomResponse>> createRoom(RoomRequest request);

  Mono<ApiResponseDTO<RoomResponse>> getRoomById(String roomId);

  Mono<ApiResponsePaginationDTO<RoomResponse>> getUserRooms(int page, int size);

  Mono<ApiResponseDTO<RoomResponse>> updateRoom(String roomId, RoomRequest request);

  Mono<ApiResponseDTO<Void>> deleteRoom(String roomId);

  Mono<ApiResponseDTO<RoomResponse>> addMembersToRoom(
      String roomId, List<Long> userIds, Long requesterId);

  Mono<ApiResponseDTO<RoomResponse>> removeMemberFromRoom(
      String roomId, Long userId, Long requesterId);

  Mono<ApiResponseDTO<RoomResponse>> promoteToAdmin(String roomId, Long userId, Long requesterId);

  Mono<ApiResponseDTO<RoomResponse>> getOrCreateDirectRoom(Long userId1, Long userId2);

  // Direct methods for internal use (return raw data without wrapper)
  Mono<RoomResponse> createRoomDirect(RoomRequest request);

  Mono<RoomResponse> getRoomByIdDirect(String roomId);

  Mono<RoomResponse> updateRoomDirect(String roomId, RoomRequest request);

  Mono<Void> deleteRoomDirect(String roomId);

  Mono<RoomResponse> addMembersToRoomDirect(String roomId, List<Long> userIds, Long requesterId);

  Mono<RoomResponse> removeMemberFromRoomDirect(String roomId, Long userId, Long requesterId);

  Mono<RoomResponse> promoteToAdminDirect(String roomId, Long userId, Long requesterId);

  Mono<RoomResponse> getOrCreateDirectRoomDirect(Long userId1, Long userId2);

  Mono<Void> updateLastMessage(String roomId, String messageId, LocalDateTime timestamp);
}
