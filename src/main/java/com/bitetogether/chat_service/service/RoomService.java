package com.bitetogether.chat_service.service;

import com.bitetogether.chat_service.dto.request.RoomDetailResponse;
import com.bitetogether.chat_service.dto.request.RoomRequest;
import com.bitetogether.chat_service.dto.request.RoomResponse;
import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.dto.ApiResponsePaginationDTO;
import java.time.LocalDateTime;
import java.util.List;
import reactor.core.publisher.Mono;

public interface RoomService {
  Mono<ApiResponseDTO<RoomResponse>> createRoom(RoomRequest request);

  Mono<ApiResponseDTO<RoomDetailResponse>> getRoomById(String roomId);

  Mono<ApiResponsePaginationDTO<RoomDetailResponse>> getUserRooms(int page, int size);

  Mono<ApiResponseDTO<RoomResponse>> updateRoom(String roomId, RoomRequest request);

  Mono<ApiResponseDTO<Void>> deleteRoom(String roomId);

  Mono<ApiResponseDTO<RoomResponse>> addMembersToRoom(
      String roomId, List<Long> userIds, Long requesterId);

  Mono<ApiResponseDTO<RoomResponse>> removeMemberFromRoom(
      String roomId, Long userId, Long requesterId);

  Mono<ApiResponseDTO<RoomResponse>> promoteToAdmin(String roomId, Long userId, Long requesterId);

  Mono<ApiResponseDTO<RoomResponse>> getOrCreateDirectRoom(Long otherUserId);

  // Direct methods for internal use (return raw data without wrapper)
  Mono<RoomResponse> createRoomDirect(RoomRequest request);

  Mono<RoomDetailResponse> getRoomByIdDirect(String roomId);

  Mono<RoomResponse> updateRoomDirect(String roomId, RoomRequest request);

  Mono<Void> deleteRoomDirect(String roomId);

  Mono<RoomResponse> addMembersToRoomDirect(String roomId, List<Long> userIds, Long requesterId);

  Mono<RoomResponse> removeMemberFromRoomDirect(String roomId, Long userId, Long requesterId);

  Mono<RoomResponse> promoteToAdminDirect(String roomId, Long userId, Long requesterId);

  Mono<RoomResponse> getOrCreateDirectRoomDirect(Long otherUserId);

  Mono<Void> updateLastMessage(String roomId, String messageId, LocalDateTime timestamp);
}
