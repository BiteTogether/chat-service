package com.bitetogether.chat_service.dto.request;

import com.bitetogether.chat_service.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request payload for creating or updating a chat message")
public class MessageRequest {
  @NotBlank
  @Schema(
      description = "Unique identifier of the chat room where the message will be sent",
      example = "room123")
  private String roomId;

  @NotBlank
  @Schema(
      description = "The actual content/text of the message",
      example = "Hello, how are you?",
      maxLength = 5000)
  private String content;

  @NotBlank
  @Schema(description = "Type of the message (TEXT, IMAGE, FILE, etc.)", example = "TEXT")
  private MessageType type;

  @NotBlank
  @Schema(
      description = "ID of the message being replied to (optional, for threaded conversations)",
      example = "507f1f77bcf86cd799439011",
      nullable = true)
  private String replyToMessageId;
}
