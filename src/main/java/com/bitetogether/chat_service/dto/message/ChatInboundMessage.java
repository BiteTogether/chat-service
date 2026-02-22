package com.bitetogether.chat_service.dto.message;

import com.bitetogether.chat_service.enums.MessageType;
import com.bitetogether.chat_service.enums.WebSocketAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatInboundMessage {

  @NotBlank private String conversationId;

  /** The WebSocket action (SUBSCRIBE, SEND, etc.) */
  @NotNull private WebSocketAction action;

  /** The type of message content (TEXT, IMAGE, FILE, EMOJI) - only required for SEND action */
  private MessageType messageType;

  /** Message content (plaintext - server will encrypt) - only required for SEND action */
  private String content;
}
