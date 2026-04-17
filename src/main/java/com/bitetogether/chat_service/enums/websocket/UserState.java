package com.bitetogether.chat_service.enums.websocket;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User connection state for presence management")
public enum UserState {
  @Schema(description = "User is actively using the app in foreground")
  FOREGROUND,

  @Schema(description = "User has app open but in background")
  BACKGROUND,

  @Schema(description = "User is offline - no active WebSocket connection")
  OFFLINE
}
