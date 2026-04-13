package com.bitetogether.chat_service.dto.message;

import com.bitetogether.chat_service.enums.MessageType;
import com.bitetogether.chat_service.enums.WebSocketAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
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

  /** Live location payload - only used for LOCATION_UPDATE action */
  private LocationPayload location;

  /** Whether the user is currently sharing location */
  private Boolean isSharing;

  public record LocationPayload(
      Double lat, Double lng, Double accuracy, Double heading, Double speed, Instant timestamp) {}
}
