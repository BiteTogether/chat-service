package com.bitetogether.chat_service.controller;

import com.bitetogether.chat_service.dto.message.ChatInboundMessage;
import com.bitetogether.chat_service.dto.message.ChatMessageDTO;
import com.bitetogether.chat_service.dto.message.MessagePageResponse;
import com.bitetogether.chat_service.dto.message.UpdateMessageRequest;
import com.bitetogether.chat_service.service.MessageService;
import com.bitetogether.common.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/messages")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Message", description = "Message management APIs")
public class MessageController {

  MessageService messageService;

  // ==================== CREATE ====================

  @PostMapping
  @Operation(
      summary = "Send a new message",
      description =
          "Creates and sends a new message to a specific conversation. "
              + "The message will be encrypted and broadcasted to all participants in real-time.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Message sent successfully",
            content = @Content(schema = @Schema(implementation = ChatMessageDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid message request",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not a participant of the conversation",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<ChatMessageDTO>>> sendMessage(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Message details to be sent",
              required = true,
              content = @Content(schema = @Schema(implementation = ChatInboundMessage.class)))
          @Valid
          @RequestBody
          ChatInboundMessage inbound) {
    return messageService
        .processIncoming(inbound)
        .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  // ==================== READ ====================

  @GetMapping("/conversation/{conversationId}")
  @Operation(
      summary = "Get messages by conversation",
      description =
          "Retrieves messages for a specific conversation with cursor-based pagination. "
              + "Messages are returned in descending order (newest first). "
              + "Use the 'cursor' parameter to fetch older messages.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Messages retrieved successfully",
            content = @Content(schema = @Schema(implementation = MessagePageResponse.class))),
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
  public Mono<ResponseEntity<ApiResponseDTO<MessagePageResponse>>> getMessagesByConversation(
      @Parameter(description = "ID of the conversation", required = true, example = "conv_xyz789")
          @PathVariable
          String conversationId,
      @Parameter(
              description = "Cursor (sequence number) to start from. Omit for first page.",
              example = "42")
          @RequestParam(required = false)
          Long cursor,
      @Parameter(description = "Number of messages to fetch (default 20, max 100)", example = "20")
          @RequestParam(required = false)
          Integer limit) {
    return messageService
        .getMessagesByConversationId(conversationId, cursor, limit)
        .map(ResponseEntity::ok);
  }

  @GetMapping("/{messageId}")
  @Operation(
      summary = "Get a message by ID",
      description =
          "Retrieves a single message by its ID. User must be a participant of the conversation.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Message retrieved successfully",
            content = @Content(schema = @Schema(implementation = ChatMessageDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not a participant of the conversation",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Message not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<ChatMessageDTO>>> getMessageById(
      @Parameter(description = "ID of the message", required = true, example = "msg_abc123")
          @PathVariable
          String messageId) {
    return messageService.getMessageById(messageId).map(ResponseEntity::ok);
  }

  // ==================== UPDATE ====================

  @PutMapping("/{messageId}")
  @Operation(
      summary = "Update a message",
      description =
          "Updates the content of a message. Only the sender can update their own message. "
              + "The update will be broadcasted to all participants in real-time.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Message updated successfully",
            content = @Content(schema = @Schema(implementation = ChatMessageDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid update request",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not authorized to update this message",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Message not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<ChatMessageDTO>>> updateMessage(
      @Parameter(
              description = "ID of the message to update",
              required = true,
              example = "msg_abc123")
          @PathVariable
          String messageId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "New message content",
              required = true,
              content = @Content(schema = @Schema(implementation = UpdateMessageRequest.class)))
          @Valid
          @RequestBody
          UpdateMessageRequest request) {
    return messageService.updateMessage(messageId, request).map(ResponseEntity::ok);
  }

  // ==================== DELETE ====================

  @DeleteMapping("/{messageId}")
  @Operation(
      summary = "Delete a message",
      description =
          "Deletes a message. Only the sender can delete their own message. "
              + "The deletion will be broadcasted to all participants in real-time.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Message deleted successfully",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not authorized to delete this message",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Message not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<Void>>> deleteMessage(
      @Parameter(
              description = "ID of the message to delete",
              required = true,
              example = "msg_abc123")
          @PathVariable
          String messageId) {
    return messageService.deleteMessage(messageId).map(ResponseEntity::ok);
  }
}
