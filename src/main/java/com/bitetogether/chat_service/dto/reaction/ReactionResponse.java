package com.bitetogether.chat_service.dto.reaction;

import com.bitetogether.chat_service.dto.base.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Response payload containing reaction details")
public class ReactionResponse extends BaseResponse {

  @Schema(description = "Unique identifier of the reaction", example = "507f1f77bcf86cd799439011")
  private String id;

  @Schema(
      description = "Unique identifier of the message that was reacted to",
      example = "507f1f77bcf86cd799439012")
  private String messageId;

  @Schema(description = "Unique identifier of the user who added the reaction", example = "123")
  private Long userId;

  @Schema(description = "The emoji character or code representing the reaction", example = "👍")
  private String emoji;
}
