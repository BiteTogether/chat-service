package com.bitetogether.chat_service.controller;

import com.bitetogether.chat_service.dto.message.ChatInboundMessage;
import com.bitetogether.chat_service.dto.message.ChatMessageDTO;
import com.bitetogether.chat_service.service.MessageService;
import com.bitetogether.common.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/messages")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Message", description = "Message management APIs")
public class MessageController {

  MessageService messageService;

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
    return messageService.processIncoming(inbound).map(ResponseEntity::ok);
  }
}
