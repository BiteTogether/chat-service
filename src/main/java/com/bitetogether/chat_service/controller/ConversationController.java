package com.bitetogether.chat_service.controller;

import com.bitetogether.chat_service.dto.conversation.ConversationDTO;
import com.bitetogether.chat_service.dto.conversation.CreateConversationRequest;
import com.bitetogether.chat_service.dto.conversation.ParticipantDTO;
import com.bitetogether.chat_service.dto.conversation.UpdateConversationRequest;
import com.bitetogether.chat_service.enums.Role;
import com.bitetogether.chat_service.service.ConversationService;
import com.bitetogether.common.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/conversations")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Conversation", description = "Conversation management APIs")
public class ConversationController {

  ConversationService conversationService;

  // ==================== CREATE ====================

  @PostMapping
  @Operation(
      summary = "Create a new conversation",
      description =
          "Creates a new conversation (DIRECT or GROUP). For DIRECT conversations, exactly 2 participants are required. "
              + "For GROUP conversations, a name is required. The current user is automatically added as a participant with ADMIN role.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Conversation created successfully",
            content = @Content(schema = @Schema(implementation = ConversationDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - e.g., DIRECT conversation requires exactly 2 participants, GROUP requires a name",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - User not authenticated",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<ConversationDTO>>> createConversation(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Conversation details to be created",
              required = true,
              content = @Content(schema = @Schema(implementation = CreateConversationRequest.class)))
          @Valid
          @RequestBody
          CreateConversationRequest request) {
    return conversationService
        .createConversation(request)
        .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  // ==================== READ ====================

  @GetMapping("/{conversationId}")
  @Operation(
      summary = "Get a conversation by ID",
      description = "Retrieves detailed information about a specific conversation including participants and unread count.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Conversation retrieved successfully",
            content = @Content(schema = @Schema(implementation = ConversationDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not a participant of the conversation",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Conversation not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<ConversationDTO>>> getConversationById(
      @Parameter(description = "Conversation ID", required = true, example = "conv_abc123")
          @PathVariable
          String conversationId) {
    return conversationService.getConversationById(conversationId).map(ResponseEntity::ok);
  }

  @GetMapping
  @Operation(
      summary = "Get all conversations for current user",
      description = "Retrieves all conversations that the current user is a participant of, including unread counts.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Conversations retrieved successfully",
            content = @Content(schema = @Schema(implementation = ConversationDTO.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - User not authenticated",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<List<ConversationDTO>>>> getMyConversations() {
    return conversationService.getMyConversations().map(ResponseEntity::ok);
  }

  // ==================== UPDATE ====================

  @PutMapping("/{conversationId}")
  @Operation(
      summary = "Update a conversation",
      description = "Updates conversation details such as name and avatar. Only ADMIN participants can perform this action.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Conversation updated successfully",
            content = @Content(schema = @Schema(implementation = ConversationDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not an admin of the conversation",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Conversation not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<ConversationDTO>>> updateConversation(
      @Parameter(description = "Conversation ID", required = true, example = "conv_abc123")
          @PathVariable
          String conversationId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Updated conversation details",
              required = true,
              content = @Content(schema = @Schema(implementation = UpdateConversationRequest.class)))
          @Valid
          @RequestBody
          UpdateConversationRequest request) {
    return conversationService.updateConversation(conversationId, request).map(ResponseEntity::ok);
  }

  // ==================== PARTICIPANT MANAGEMENT ====================

  @PostMapping("/{conversationId}/participants/{userId}")
  @Operation(
      summary = "Add a participant to a conversation",
      description = "Adds a new participant to the conversation. Only ADMIN participants can add new members.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Participant added successfully",
            content = @Content(schema = @Schema(implementation = ParticipantDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "User is already a participant",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not an admin of the conversation",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Conversation not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<ParticipantDTO>>> addParticipant(
      @Parameter(description = "Conversation ID", required = true, example = "conv_abc123")
          @PathVariable
          String conversationId,
      @Parameter(description = "User ID to add as participant", required = true, example = "123")
          @PathVariable
          Long userId) {
    return conversationService
        .addParticipant(conversationId, userId)
        .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @DeleteMapping("/{conversationId}/participants/{userId}")
  @Operation(
      summary = "Remove a participant from a conversation",
      description =
          "Removes a participant from the conversation. Users can remove themselves, or ADMINs can remove other participants. "
              + "The last ADMIN cannot be removed.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Participant removed successfully",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Cannot remove the last admin",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not authorized to remove this participant",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Conversation or participant not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<Void>>> removeParticipant(
      @Parameter(description = "Conversation ID", required = true, example = "conv_abc123")
          @PathVariable
          String conversationId,
      @Parameter(description = "User ID to remove", required = true, example = "123")
          @PathVariable
          Long userId) {
    return conversationService.removeParticipant(conversationId, userId).map(ResponseEntity::ok);
  }

  @PatchMapping("/{conversationId}/participants/{userId}/role")
  @Operation(
      summary = "Update participant role",
      description =
          "Updates the role of a participant in the conversation. Only ADMIN participants can change roles. "
              + "The last ADMIN cannot be demoted to MEMBER.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Participant role updated successfully",
            content = @Content(schema = @Schema(implementation = ParticipantDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Cannot demote the last admin",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not an admin of the conversation",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Conversation or participant not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<ParticipantDTO>>> updateParticipantRole(
      @Parameter(description = "Conversation ID", required = true, example = "conv_abc123")
          @PathVariable
          String conversationId,
      @Parameter(description = "User ID to update", required = true, example = "123")
          @PathVariable
          Long userId,
      @Parameter(description = "New role for the participant", required = true, example = "ADMIN")
          @RequestParam
          Role role) {
    return conversationService
        .updateParticipantRole(conversationId, userId, role)
        .map(ResponseEntity::ok);
  }

  // ==================== READ RECEIPTS ====================

  @PostMapping("/{conversationId}/read")
  @Operation(
      summary = "Mark messages as read",
      description =
          "Marks all messages up to the specified sequence number as read for the current user.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Messages marked as read successfully",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not a participant of the conversation",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Conversation not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<Void>>> markAsRead(
      @Parameter(description = "Conversation ID", required = true, example = "conv_abc123")
          @PathVariable
          String conversationId,
      @Parameter(
              description = "Last read message sequence number",
              required = true,
              example = "42")
          @RequestParam
          Long lastReadSequence) {
    return conversationService.markAsRead(conversationId, lastReadSequence).map(ResponseEntity::ok);
  }

  // ==================== DELETE ====================

  @DeleteMapping("/{conversationId}")
  @Operation(
      summary = "Delete a conversation",
      description =
          "Permanently deletes a conversation and all associated data (messages, participants). "
              + "Only ADMIN participants can delete conversations.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Conversation deleted successfully",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not an admin of the conversation",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Conversation not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<Void>>> deleteConversation(
      @Parameter(description = "Conversation ID", required = true, example = "conv_abc123")
          @PathVariable
          String conversationId) {
    return conversationService.deleteConversation(conversationId).map(ResponseEntity::ok);
  }
}

