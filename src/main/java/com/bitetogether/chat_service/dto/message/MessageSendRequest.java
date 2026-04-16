package com.bitetogether.chat_service.dto.message;

import com.bitetogether.chat_service.enums.message.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MessageSendRequest {
  @NotBlank private String conversationId;

  @NotNull private MessageType messageType;

  @NotBlank private String content;
}
