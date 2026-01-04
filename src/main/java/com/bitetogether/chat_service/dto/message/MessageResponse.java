package com.bitetogether.chat_service.dto.message;

import com.bitetogether.chat_service.dto.base.BaseResponse;
import com.bitetogether.chat_service.dto.user.SenderInfo;
import com.bitetogether.chat_service.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Response payload containing chat message details")
public class MessageResponse extends BaseResponse {

  @Schema(description = "Unique identifier of the message", example = "507f1f77bcf86cd799439011")
  private String id;

  @Schema(
      description = "Unique identifier of the chat room this message belongs to",
      example = "room123")
  private String roomId;

  @Schema(description = "The actual content/text of the message", example = "Hello, how are you?")
  private String content;

  @Schema(description = "Type of the message (TEXT, IMAGE, FILE, etc.)", example = "TEXT")
  private MessageType type;

  @Schema(description = "Details of the user who sent this message")
  private SenderInfo sender;

  @Schema(
      description = "The original message being replied to (if this is a reply)",
      nullable = true)
  private MessageResponse replyTo;

  @Schema(
      description =
          "Flag indicating whether this message has been deleted. Used for real-time deletion events in streams.",
      example = "false",
      defaultValue = "false")
  private Boolean deleted = false;
}
