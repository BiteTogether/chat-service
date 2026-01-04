package com.bitetogether.chat_service.controller;

import com.bitetogether.chat_service.dto.message.MessageRequest;
import com.bitetogether.chat_service.dto.message.MessageResponse;
import com.bitetogether.chat_service.service.MessageService;
import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.dto.ApiResponsePaginationDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Message Management", description = "REST API endpoints for managing chat messages")
public class MessageController {

  MessageService messageService;

  @PostMapping
  @Operation(
      summary = "Send a new message",
      description =
          "Creates and sends a new message to a specific chat room. The message will be broadcasted to all subscribers of the room in real-time.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Message sent successfully",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid message request",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public Mono<ResponseEntity<ApiResponseDTO<MessageResponse>>> sendMessage(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Message details to be sent",
              required = true,
              content = @Content(schema = @Schema(implementation = MessageRequest.class)))
          @RequestBody
          MessageRequest request) {
    return messageService.sendMessage(request).map(ResponseEntity::ok);
  }

  @PutMapping("/{messageId}")
  @Operation(
      summary = "Update an existing message",
      description =
          "Updates the content or type of an existing message. The updated message will be broadcasted to all subscribers in real-time.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Message updated successfully",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid message ID or request"),
        @ApiResponse(responseCode = "404", description = "Message not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public Mono<ResponseEntity<ApiResponseDTO<MessageResponse>>> updateMessage(
      @Parameter(
              description = "ID of the message to update",
              required = true,
              example = "507f1f77bcf86cd799439011")
          @PathVariable
          String messageId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Updated message details",
              required = true,
              content = @Content(schema = @Schema(implementation = MessageRequest.class)))
          @RequestBody
          MessageRequest request) {
    return messageService.updateMessage(messageId, request).map(ResponseEntity::ok);
  }

  @DeleteMapping("/{messageId}")
  @Operation(
      summary = "Delete a message",
      description =
          "Deletes a message from the chat room. A deletion event will be broadcasted to all subscribers in real-time.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Message deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid message ID"),
        @ApiResponse(responseCode = "404", description = "Message not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public Mono<ResponseEntity<ApiResponseDTO<Void>>> deleteMessage(
      @Parameter(
              description = "ID of the message to delete",
              required = true,
              example = "507f1f77bcf86cd799439011")
          @PathVariable
          String messageId) {
    return messageService.deleteMessage(messageId).map(ResponseEntity::ok);
  }

  @GetMapping("/{messageId}")
  @Operation(
      summary = "Get message by ID",
      description = "Retrieves a specific message by its unique identifier")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Message retrieved successfully",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid message ID"),
        @ApiResponse(responseCode = "404", description = "Message not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public Mono<ResponseEntity<ApiResponseDTO<MessageResponse>>> getMessageById(
      @Parameter(
              description = "ID of the message to retrieve",
              required = true,
              example = "507f1f77bcf86cd799439011")
          @PathVariable
          String messageId) {
    return messageService.getMessageById(messageId).map(ResponseEntity::ok);
  }

  @GetMapping("/room/{roomId}")
  @Operation(
      summary = "Get messages in a room with pagination",
      description =
          "Retrieves messages in a specific chat room with pagination support. Returns page metadata including total count and total pages.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Messages retrieved successfully",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid room ID or pagination parameters"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public Mono<ResponseEntity<ApiResponsePaginationDTO<MessageResponse>>> getMessagesByRoomPaginated(
      @Parameter(description = "ID of the chat room", required = true, example = "room123")
          @PathVariable
          String roomId,
      @Parameter(description = "Page number (0-indexed)", example = "0")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Number of items per page", example = "20")
          @RequestParam(defaultValue = "20")
          int size) {
    return messageService.getMessagesByRoomPaginated(roomId, page, size).map(ResponseEntity::ok);
  }

  @GetMapping(value = "/room/{roomId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(
      summary = "Stream messages in real-time",
      description =
          "Opens a Server-Sent Events (SSE) stream to receive real-time message updates for a specific room. "
              + "This includes existing messages followed by new messages, updates, and deletions as they occur.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Stream established successfully",
            content =
                @Content(
                    mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                    schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid room ID"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public Flux<ApiResponseDTO<MessageResponse>> streamMessages(
      @Parameter(
              description = "ID of the chat room to stream messages from",
              required = true,
              example = "room123")
          @PathVariable
          String roomId) {
    return messageService.streamMessages(roomId);
  }

  @GetMapping("/{messageId}/replies")
  @Operation(
      summary = "Get all replies to a message",
      description =
          "Retrieves all messages that are replies to a specific message. This is useful for implementing threaded conversations and showing reply chains.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Replies retrieved successfully",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid message ID"),
        @ApiResponse(responseCode = "404", description = "Message not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
      })
  public Flux<ResponseEntity<ApiResponseDTO<MessageResponse>>> getMessageReplies(
      @Parameter(
              description = "ID of the message to get replies for",
              required = true,
              example = "507f1f77bcf86cd799439011")
          @PathVariable
          String messageId) {
    return messageService.getMessageReplies(messageId).map(ResponseEntity::ok);
  }
}
