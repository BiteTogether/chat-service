package com.bitetogether.chat_service.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Response payload containing reaction details")
public class ReactionResponse {

  @Schema(description = "Unique identifier of the reaction", example = "507f1f77bcf86cd799439011")
  private String id;

  @Schema(
      description = "Unique identifier of the message that was reacted to",
      example = "507f1f77bcf86cd799439012")
  private String messageId;

  @Schema(description = "Unique identifier of the user who added the reaction", example = "user123")
  private String userId;

  @Schema(description = "The emoji character or code representing the reaction", example = "👍")
  private String emoji;
}
