package com.bitetogether.chat_service.controller;

import com.bitetogether.chat_service.dto.request.RoomRequest;
import com.bitetogether.chat_service.dto.response.RoomResponse;
import com.bitetogether.chat_service.service.inter.RoomService;
import com.bitetogether.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(
    name = "Room Management",
    description = "REST API endpoints for managing chat rooms (direct and group chats)")
public class RoomController {

  RoomService roomService;

  @PostMapping
  @Operation(
      summary = "Create a new chat room",
      description =
          "Creates a new chat room. Can be either a DIRECT chat (1-on-1 between 2 users) or a GROUP chat (multiple users). "
              + "For GROUP rooms, the name is required and the creator becomes an admin by default.")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Room created successfully",
            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid room request - validation errors"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error")
      })
  public Mono<ResponseEntity<ApiResponse<RoomResponse>>> createRoom(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Room details to create",
              required = true,
              content = @Content(schema = @Schema(implementation = RoomRequest.class)))
          @RequestBody
          RoomRequest request) {
    return roomService.createRoom(request).map(ResponseEntity::ok);
  }

  @GetMapping("/{roomId}")
  @Operation(
      summary = "Get room by ID",
      description =
          "Retrieves detailed information about a specific chat room by its unique identifier")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Room retrieved successfully",
            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error")
      })
  public Mono<ResponseEntity<ApiResponse<RoomResponse>>> getRoomById(
      @Parameter(
              description = "ID of the room to retrieve",
              required = true,
              example = "507f1f77bcf86cd799439011")
          @PathVariable
          String roomId) {
    return roomService.getRoomById(roomId).map(ResponseEntity::ok);
  }

  @GetMapping("/user/{userId}")
  @Operation(
      summary = "Get all rooms for a user",
      description =
          "Retrieves all chat rooms where the specified user is a member. "
              + "Results are ordered by the timestamp of the last message (most recent first).")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Rooms retrieved successfully",
            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid user ID"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error")
      })
  public Flux<ResponseEntity<ApiResponse<RoomResponse>>> getUserRooms(
      @Parameter(description = "ID of the user", required = true, example = "123") @PathVariable
          Long userId) {
    return roomService.getUserRooms(userId).map(ResponseEntity::ok);
  }

  @PutMapping("/{roomId}")
  @Operation(
      summary = "Update room details",
      description =
          "Updates a chat room's details such as name, avatar, or other metadata. "
              + "Only admins can update GROUP rooms.")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Room updated successfully",
            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid room ID or request"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error")
      })
  public Mono<ResponseEntity<ApiResponse<RoomResponse>>> updateRoom(
      @Parameter(
              description = "ID of the room to update",
              required = true,
              example = "507f1f77bcf86cd799439011")
          @PathVariable
          String roomId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Updated room details",
              required = true,
              content = @Content(schema = @Schema(implementation = RoomRequest.class)))
          @RequestBody
          RoomRequest request) {
    return roomService.updateRoom(roomId, request).map(ResponseEntity::ok);
  }

  @DeleteMapping("/{roomId}")
  @Operation(
      summary = "Delete a room",
      description =
          "Deletes a chat room permanently. All messages in the room should be handled separately. "
              + "Only admins can delete GROUP rooms.")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Room deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid room ID"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error")
      })
  public Mono<ResponseEntity<ApiResponse<Void>>> deleteRoom(
      @Parameter(
              description = "ID of the room to delete",
              required = true,
              example = "507f1f77bcf86cd799439011")
          @PathVariable
          String roomId) {
    return roomService.deleteRoom(roomId).map(ResponseEntity::ok);
  }

  @PostMapping("/{roomId}/members")
  @Operation(
      summary = "Add members to a room",
      description =
          "Adds one or more users to a chat room. For GROUP rooms, only admins can add new members. "
              + "The requester must be specified to verify permissions.")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Members added successfully",
            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request or insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error")
      })
  public Mono<ResponseEntity<ApiResponse<RoomResponse>>> addMembersToRoom(
      @Parameter(
              description = "ID of the room to add members to",
              required = true,
              example = "507f1f77bcf86cd799439011")
          @PathVariable
          String roomId,
      @Parameter(
              description = "List of user IDs to add to the room",
              required = true,
              example = "[123, 456]")
          @RequestBody
          List<Long> userIds,
      @Parameter(
              description = "ID of the user making the request (for permission checking)",
              required = true,
              example = "123")
          @RequestParam
          Long requesterId) {
    return roomService.addMembersToRoom(roomId, userIds, requesterId).map(ResponseEntity::ok);
  }

  @DeleteMapping("/{roomId}/members/{userId}")
  @Operation(
      summary = "Remove a member from a room",
      description =
          "Removes a user from a chat room. Admins can remove any member, or users can remove themselves. "
              + "If the last admin leaves a GROUP room, the room may become orphaned.")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Member removed successfully",
            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request or insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error")
      })
  public Mono<ResponseEntity<ApiResponse<RoomResponse>>> removeMemberFromRoom(
      @Parameter(
              description = "ID of the room",
              required = true,
              example = "507f1f77bcf86cd799439011")
          @PathVariable
          String roomId,
      @Parameter(
              description = "ID of the user to remove from the room",
              required = true,
              example = "456")
          @PathVariable
          Long userId,
      @Parameter(
              description = "ID of the user making the request (for permission checking)",
              required = true,
              example = "123")
          @RequestParam
          Long requesterId) {
    return roomService.removeMemberFromRoom(roomId, userId, requesterId).map(ResponseEntity::ok);
  }

  @PostMapping("/{roomId}/admins/{userId}")
  @Operation(
      summary = "Promote a user to admin",
      description =
          "Promotes a room member to admin status. Only existing admins can promote other users. "
              + "This operation is only valid for GROUP rooms.")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "User promoted to admin successfully",
            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid request or insufficient permissions"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Room not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error")
      })
  public Mono<ResponseEntity<ApiResponse<RoomResponse>>> promoteToAdmin(
      @Parameter(
              description = "ID of the room",
              required = true,
              example = "507f1f77bcf86cd799439011")
          @PathVariable
          String roomId,
      @Parameter(
              description = "ID of the user to promote to admin",
              required = true,
              example = "456")
          @PathVariable
          Long userId,
      @Parameter(
              description = "ID of the user making the request (must be an admin)",
              required = true,
              example = "123")
          @RequestParam
          Long requesterId) {
    return roomService.promoteToAdmin(roomId, userId, requesterId).map(ResponseEntity::ok);
  }

  @GetMapping("/direct")
  @Operation(
      summary = "Get or create a direct chat room",
      description =
          "Retrieves an existing direct (1-on-1) chat room between two users, or creates one if it doesn't exist. "
              + "This ensures that there's only one direct conversation between any two users.")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Direct room retrieved or created successfully",
            content = @Content(schema = @Schema(implementation = RoomResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid user IDs"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Internal server error")
      })
  public Mono<ResponseEntity<ApiResponse<RoomResponse>>> getOrCreateDirectRoom(
      @Parameter(description = "ID of the first user", required = true, example = "123")
          @RequestParam
          Long userId1,
      @Parameter(description = "ID of the second user", required = true, example = "456")
          @RequestParam
          Long userId2) {
    return roomService.getOrCreateDirectRoom(userId1, userId2).map(ResponseEntity::ok);
  }
}
