package com.bitetogether.chat_service.enums.websocket;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of WebSocket action/command")
public enum WebSocketAction {
  @Schema(description = "Subscribe to a conversation room to receive messages")
  SUBSCRIBE,

  @Schema(description = "Send a message to a conversation room")
  SEND,

  @Schema(description = "Unsubscribe from a conversation room")
  UNSUBSCRIBE,

  @Schema(description = "Typing indicator")
  TYPING,

  @Schema(description = "Mark messages as read")
  READ,

  @Schema(description = "Share live location update in a conversation room")
  LOCATION_UPDATE,

  @Schema(description = "Vote session realtime updates in a conversation room")
  VOTE_UPDATE,

  @Schema(description = "Bill session realtime updates in a conversation room")
  BILL_UPDATE
}
