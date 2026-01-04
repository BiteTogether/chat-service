package com.bitetogether.chat_service.service.impl;

import com.bitetogether.chat_service.client.webclient.UserClient;
import com.bitetogether.chat_service.dto.request.RoomDetailResponse;
import com.bitetogether.chat_service.dto.request.RoomRequest;
import com.bitetogether.chat_service.dto.request.RoomResponse;
import com.bitetogether.chat_service.enums.RoomType;
import com.bitetogether.chat_service.exception.ErrorCode;
import com.bitetogether.chat_service.mapper.RoomMapper;
import com.bitetogether.chat_service.model.Room;
import com.bitetogether.chat_service.repository.RoomRepository;
import com.bitetogether.chat_service.service.RoomService;
import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.dto.ApiResponsePaginationDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.common.util.ApiResponseUtil;
import com.bitetogether.common.util.ReactiveUserContextUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomServiceImpl implements RoomService {

  RoomRepository roomRepository;
  RoomMapper roomMapper;
  UserClient userClient;

  @Override
  public Mono<ApiResponseDTO<RoomResponse>> createRoom(RoomRequest request) {
    log.info("REST: Creating room of type: {}", request.getRoomType());
    return createRoomDirect(request)
        .map(
            response ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.SUCCESS, "Room created successfully", response))
        .onErrorResume(
            e -> {
              log.error("Failed to create room: {}", e.getMessage());
              return Mono.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST,
                      "Failed to create room: " + e.getMessage(),
                      null));
            });
  }

  @Override
  public Mono<ApiResponseDTO<RoomDetailResponse>> getRoomById(String roomId) {
    log.info("REST: Getting room by id: {}", roomId);
    return getRoomByIdDirect(roomId)
        .map(
            response ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.SUCCESS, "Room retrieved successfully", response))
        .onErrorResume(
            e -> {
              log.error("Failed to get room: {}", e.getMessage());
              return Mono.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST, "Room not found: " + e.getMessage(), null));
            });
  }

  @Override
  public Mono<ApiResponseDTO<RoomResponse>> updateRoom(String roomId, RoomRequest request) {
    log.info("REST: Updating room: {}", roomId);
    return updateRoomDirect(roomId, request)
        .map(
            response ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.SUCCESS, "Room updated successfully", response))
        .onErrorResume(
            e -> {
              log.error("Failed to update room: {}", e.getMessage());
              return Mono.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST,
                      "Failed to update room: " + e.getMessage(),
                      null));
            });
  }

  @Override
  public Mono<ApiResponseDTO<Void>> deleteRoom(String roomId) {
    log.info("REST: Deleting room: {}", roomId);
    return deleteRoomDirect(roomId)
        .then(
            Mono.fromCallable(
                () ->
                    ApiResponseUtil.<Void>buildApiResponse(
                        ApiResponseStatus.SUCCESS, "Room deleted successfully", null)))
        .onErrorResume(
            e -> {
              log.error("Failed to delete room: {}", e.getMessage());
              return Mono.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST,
                      "Failed to delete room: " + e.getMessage(),
                      null));
            });
  }

  @Override
  public Mono<ApiResponseDTO<RoomResponse>> addMembersToRoom(
      String roomId, List<Long> userIds, Long requesterId) {
    log.info("REST: Adding members to room: {}", roomId);
    return addMembersToRoomDirect(roomId, userIds, requesterId)
        .map(
            response ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.SUCCESS, "Members added successfully", response))
        .onErrorResume(
            e -> {
              log.error("Failed to add members: {}", e.getMessage());
              return Mono.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST,
                      "Failed to add members: " + e.getMessage(),
                      null));
            });
  }

  @Override
  public Mono<ApiResponseDTO<RoomResponse>> removeMemberFromRoom(
      String roomId, Long userId, Long requesterId) {
    log.info("REST: Removing member {} from room: {}", userId, roomId);
    return removeMemberFromRoomDirect(roomId, userId, requesterId)
        .map(
            response ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.SUCCESS, "Member removed successfully", response))
        .onErrorResume(
            e -> {
              log.error("Failed to remove member: {}", e.getMessage());
              return Mono.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST,
                      "Failed to remove member: " + e.getMessage(),
                      null));
            });
  }

  @Override
  public Mono<ApiResponseDTO<RoomResponse>> promoteToAdmin(
      String roomId, Long userId, Long requesterId) {
    log.info("REST: Promoting user {} to admin in room: {}", userId, roomId);
    return promoteToAdminDirect(roomId, userId, requesterId)
        .map(
            response ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.SUCCESS, "User promoted to admin successfully", response))
        .onErrorResume(
            e -> {
              log.error("Failed to promote user: {}", e.getMessage());
              return Mono.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST,
                      "Failed to promote user: " + e.getMessage(),
                      null));
            });
  }

  @Override
  public Mono<ApiResponseDTO<RoomResponse>> getOrCreateDirectRoom(Long userId1, Long userId2) {
    log.info("REST: Getting or creating direct room between {} and {}", userId1, userId2);
    return getOrCreateDirectRoomDirect(userId1, userId2)
        .map(
            response ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.SUCCESS,
                    "Direct room retrieved/created successfully",
                    response))
        .onErrorResume(
            e -> {
              log.error("Failed to get/create direct room: {}", e.getMessage());
              return Mono.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST,
                      "Failed to get/create direct room: " + e.getMessage(),
                      null));
            });
  }

  @Override
  public Mono<ApiResponsePaginationDTO<RoomDetailResponse>> getUserRooms(int page, int size) {
    log.info("REST: Getting paginated rooms for user (page: {}, size: {})", page, size);

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastMessageAt"));

    return ReactiveUserContextUtils.getUserIdOrError()
        .flatMap(
            userId -> {
              log.info("Getting rooms for user: {}", userId);

              Mono<Long> totalMono = roomRepository.countByUserIdsContaining(userId);

              Mono<List<RoomDetailResponse>> contentMono =
                  roomRepository
                      .findByUserIdsContaining(userId, pageable)
                      .flatMap(
                          room -> {
                            RoomDetailResponse response = roomMapper.toRoomDetailResponse(room);

                            // Fetch user details for each room
                            return userClient
                                .getListUsersByIds(room.getUserIds())
                                .map(
                                    listUserDetailDTO -> {
                                      response.setMembers(listUserDetailDTO.getUsers());
                                      return response;
                                    })
                                .defaultIfEmpty(
                                    response); // Return response even if user service fails
                          })
                      .collectList();

              return Mono.zip(totalMono, contentMono)
                  .map(
                      tuple -> {
                        long total = tuple.getT1();
                        java.util.List<RoomDetailResponse> content = tuple.getT2();
                        int totalPages = (int) Math.ceil((double) total / size);

                        return ApiResponseUtil.buildApiResponse(
                            ApiResponseStatus.SUCCESS,
                            "Rooms retrieved successfully",
                            content,
                            page,
                            totalPages,
                            total);
                      });
            })
        .onErrorResume(
            e -> {
              log.error("Failed to get paginated rooms: {}", e.getMessage());
              return Mono.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST,
                      "Failed to fetch rooms: " + e.getMessage(),
                      java.util.Collections.emptyList(),
                      page,
                      0,
                      0L));
            });
  }

  @Override
  public Mono<RoomResponse> createRoomDirect(RoomRequest request) {
    log.info("Direct: Creating room of type: {}", request.getRoomType());

    // Validate request based on room type
    if (request.getRoomType() == RoomType.GROUP
        && (request.getName() == null || request.getName().isEmpty())) {
      return Mono.error(new AppException(ErrorCode.ROOM_NAME_REQUIRED));
    }

    if (request.getRoomType() == RoomType.DIRECT && request.getUserIds().size() != 2) {
      return Mono.error(new AppException(ErrorCode.ROOM_DIRECT_TWO_USERS_REQUIRED));
    }

    return ReactiveUserContextUtils.getUserIdOrError()
        .flatMap(
            currentUserId -> {
              Room room = roomMapper.toRoom(request);

              // Ensure creator is included in userIds
              List<Long> userIds =
                  room.getUserIds() != null
                      ? new ArrayList<>(room.getUserIds())
                      : new ArrayList<>();

              if (!userIds.contains(currentUserId)) {
                userIds.add(currentUserId);
                room.setUserIds(userIds);
                log.debug("Added creator userId={} to room userIds", currentUserId);
              }

              // Always set creator as admin (for both GROUP and DIRECT rooms)
              // Ignore adminIds from request - creator is automatically admin
              room.setAdminIds(Collections.singletonList(currentUserId));
              log.debug("Set creator userId={} as room admin", currentUserId);

              return roomRepository.save(room);
            })
        .map(roomMapper::toRoomResponse)
        .doOnNext(response -> log.info("Room created with id: {}", response.getId()))
        .doOnError(e -> log.error("Error creating room: {}", e.getMessage()));
  }

  @Override
  public Mono<RoomDetailResponse> getRoomByIdDirect(String roomId) {
    log.info("Direct: Getting room by id: {}", roomId);

    return roomRepository
        .findById(roomId)
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.ROOM_NOT_FOUND)))
        .flatMap(
            room -> {
              RoomDetailResponse response = roomMapper.toRoomDetailResponse(room);

              // Fetch user details reactively
              return userClient
                  .getListUsersByIds(room.getUserIds())
                  .map(
                      listUserDetailDTO -> {
                        response.setMembers(listUserDetailDTO.getUsers());
                        return response;
                      })
                  .defaultIfEmpty(response);
            })
        .doOnError(e -> log.error("Error getting room: {}", e.getMessage()));
  }

  @Override
  public Mono<RoomResponse> updateRoomDirect(String roomId, RoomRequest request) {
    log.info("Direct: Updating room: {}", roomId);

    return roomRepository
        .findById(roomId)
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.ROOM_NOT_FOUND)))
        .flatMap(
            existing -> {
              roomMapper.updateRoomFromRoomRequest(request, existing);
              return roomRepository.save(existing);
            })
        .map(roomMapper::toRoomResponse)
        .doOnNext(response -> log.info("Room updated: {}", response.getId()))
        .doOnError(e -> log.error("Error updating room: {}", e.getMessage()));
  }

  @Override
  public Mono<Void> deleteRoomDirect(String roomId) {
    log.info("Direct: Deleting room: {}", roomId);

    return roomRepository
        .findById(roomId)
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.ROOM_NOT_FOUND)))
        .flatMap(roomRepository::delete)
        .doOnSuccess(v -> log.info("Room deleted: {}", roomId))
        .doOnError(e -> log.error("Error deleting room: {}", e.getMessage()));
  }

  @Override
  public Mono<RoomResponse> addMembersToRoomDirect(
      String roomId, List<Long> userIds, Long requesterId) {
    log.info("Direct: Adding members to room: {}", roomId);

    return roomRepository
        .findById(roomId)
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.ROOM_NOT_FOUND)))
        .flatMap(
            room -> {
              if (room.getRoomType() == RoomType.GROUP
                  && (room.getAdminIds() == null || !room.getAdminIds().contains(requesterId))) {
                return Mono.error(new AppException(ErrorCode.ROOM_ADMIN_REQUIRED));
              }

              List<Long> currentUserIds =
                  room.getUserIds() != null
                      ? new ArrayList<>(room.getUserIds())
                      : new ArrayList<>();
              for (Long userId : userIds) {
                if (!currentUserIds.contains(userId)) {
                  currentUserIds.add(userId);
                }
              }
              room.setUserIds(currentUserIds);

              return roomRepository.save(room);
            })
        .map(roomMapper::toRoomResponse)
        .doOnNext(response -> log.info("Members added to room: {}", response.getId()))
        .doOnError(e -> log.error("Error adding members: {}", e.getMessage()));
  }

  @Override
  public Mono<RoomResponse> removeMemberFromRoomDirect(
      String roomId, Long userId, Long requesterId) {
    log.info("Direct: Removing member {} from room: {}", userId, roomId);

    return roomRepository
        .findById(roomId)
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.ROOM_NOT_FOUND)))
        .flatMap(
            room -> {
              // Check if requester is admin or removing themselves
              if (room.getRoomType() == RoomType.GROUP) {
                boolean isAdmin =
                    room.getAdminIds() != null && room.getAdminIds().contains(requesterId);
                boolean isSelf = userId.equals(requesterId);

                if (!isAdmin && !isSelf) {
                  return Mono.error(new AppException(ErrorCode.ROOM_REMOVE_MEMBER_UNAUTHORIZED));
                }
              }

              // Remove member
              if (room.getUserIds() != null) {
                room.getUserIds().remove(userId);
              }

              // Remove from admins if present
              if (room.getAdminIds() != null) {
                room.getAdminIds().remove(userId);
              }

              return roomRepository.save(room);
            })
        .map(roomMapper::toRoomResponse)
        .doOnNext(response -> log.info("Member removed from room: {}", response.getId()))
        .doOnError(e -> log.error("Error removing member: {}", e.getMessage()));
  }

  @Override
  public Mono<RoomResponse> promoteToAdminDirect(String roomId, Long userId, Long requesterId) {
    log.info("Direct: Promoting user {} to admin in room: {}", userId, roomId);

    return roomRepository
        .findById(roomId)
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.ROOM_NOT_FOUND)))
        .flatMap(
            room -> {
              // Only group rooms can have multiple admins
              if (room.getRoomType() != RoomType.GROUP) {
                return Mono.error(new AppException(ErrorCode.ROOM_ONLY_GROUP_HAS_ADMINS));
              }

              // Check if requester is admin
              if (room.getAdminIds() == null || !room.getAdminIds().contains(requesterId)) {
                return Mono.error(new AppException(ErrorCode.ROOM_PROMOTE_ADMIN_UNAUTHORIZED));
              }

              // Check if user is a member
              if (room.getUserIds() == null || !room.getUserIds().contains(userId)) {
                return Mono.error(new AppException(ErrorCode.ROOM_USER_NOT_MEMBER));
              }

              // Add to admins if not already
              List<Long> adminIds = new ArrayList<>(room.getAdminIds());
              if (!adminIds.contains(userId)) {
                adminIds.add(userId);
                room.setAdminIds(adminIds);
              }

              return roomRepository.save(room);
            })
        .map(roomMapper::toRoomResponse)
        .doOnNext(response -> log.info("User promoted to admin in room: {}", response.getId()))
        .doOnError(e -> log.error("Error promoting user: {}", e.getMessage()));
  }

  @Override
  public Mono<RoomResponse> getOrCreateDirectRoomDirect(Long userId1, Long userId2) {
    log.info("Direct: Getting or creating direct room between {} and {}", userId1, userId2);

    List<Long> userIds = new ArrayList<>();
    userIds.add(userId1);
    userIds.add(userId2);
    Collections.sort(userIds); // Sort to ensure consistent ordering

    // Try to find existing direct room
    return roomRepository
        .findByRoomTypeAndUserIdsContaining(RoomType.DIRECT, userId1)
        .filter(room -> room.getUserIds() != null && room.getUserIds().contains(userId2))
        .next()
        .switchIfEmpty(
            // Create new direct room if not found
            Mono.defer(
                () -> {
                  RoomRequest request = new RoomRequest();
                  request.setRoomType(RoomType.DIRECT);
                  request.setUserIds(userIds);
                  Room room = roomMapper.toRoom(request);
                  return roomRepository.save(room);
                }))
        .map(roomMapper::toRoomResponse) // Convert Room to RoomResponse after getting/creating
        .doOnNext(response -> log.info("Direct room retrieved/created: {}", response.getId()))
        .doOnError(e -> log.error("Error getting/creating direct room: {}", e.getMessage()));
  }

  @Override
  public Mono<Void> updateLastMessage(String roomId, String messageId, LocalDateTime timestamp) {
    log.info("Direct: Updating last message for room: {}", roomId);

    return roomRepository
        .findById(roomId)
        .flatMap(
            room -> {
              room.setLastMessageId(messageId);
              room.setLastMessageAt(timestamp);
              return roomRepository.save(room);
            })
        .then()
        .doOnSuccess(v -> log.info("Last message updated for room: {}", roomId))
        .doOnError(e -> log.error("Error updating last message: {}", e.getMessage()));
  }
}
