package com.bitetogether.chat_service.dto.message;

import com.bitetogether.chat_service.dto.base.BaseResponse;
import com.bitetogether.chat_service.enums.message.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Chat message DTO")
public class ChatMessageDTO extends BaseResponse {

  @Schema(description = "Message ID", example = "msg_abc123")
  private String id;

  @Schema(description = "Conversation ID", example = "conv_xyz789")
  private String conversationId;

  @Schema(description = "Message sequence number", example = "42")
  private Long seq;

  @Schema(description = "Sender user ID", example = "1")
  private Long senderId;

  @Schema(description = "Message type", example = "TEXT")
  private MessageType type;

  @Schema(description = "Decrypted message content", example = "Hello, world!")
  private String content;
}
