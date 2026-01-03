package com.bitetogether.chat_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request payload for adding or removing a reaction to a message")
public class ReactionRequest {

  @NotBlank
  @Schema(
      description = "Unique identifier of the message being reacted to",
      example = "507f1f77bcf86cd799439011")
  private String messageId;

  @NotBlank
  @Schema(description = "Unique identifier of the user adding the reaction", example = "123")
  private Long userId;

  @NotBlank
  @Schema(description = "The emoji character or code representing the reaction", example = "👍")
  private String emoji;
}
