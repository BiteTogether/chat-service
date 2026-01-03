package com.bitetogether.chat_service.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of message content")
public enum MessageType {
  @Schema(description = "Plain text message")
  TEXT,

  @Schema(description = "Image message with URL or base64")
  IMAGE,

  @Schema(description = "File attachment message")
  FILE,

  @Schema(description = "Emoji or reaction message")
  EMOJI,
  ;
}
