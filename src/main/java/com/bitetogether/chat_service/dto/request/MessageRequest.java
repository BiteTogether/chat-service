package com.bitetogether.chat_service.dto.request;

import com.bitetogether.chat_service.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request payload for creating or updating a chat message")
public class MessageRequest {

  @Schema(
      description = "Unique identifier of the chat room where the message will be sent",
      example = "room123",
      required = true)
  private String roomId;

  @Schema(
      description = "Unique identifier of the user sending the message",
      example = "456",
      required = true)
  private Long senderId;

  @Schema(
      description = "The actual content/text of the message",
      example = "Hello, how are you?",
      required = true,
      maxLength = 5000)
  private String content;

  @Schema(
      description = "Type of the message (TEXT, IMAGE, FILE, etc.)",
      example = "TEXT",
      required = true)
  private MessageType type;

  @Schema(
      description = "ID of the message being replied to (optional, for threaded conversations)",
      example = "507f1f77bcf86cd799439011",
      nullable = true)
  private String replyToMessageId;
}
