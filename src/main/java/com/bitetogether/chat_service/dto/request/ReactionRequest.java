package com.bitetogether.chat_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request payload for adding or removing a reaction to a message")
public class ReactionRequest {

  @Schema(
      description = "Unique identifier of the message being reacted to",
      example = "507f1f77bcf86cd799439011",
      required = true)
  private String messageId;

  @Schema(
      description = "Unique identifier of the user adding the reaction",
      example = "123",
      required = true)
  private Long userId;

  @Schema(
      description = "The emoji character or code representing the reaction",
      example = "👍",
      required = true)
  private String emoji;
}
