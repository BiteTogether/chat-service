package com.bitetogether.chat_service.dto.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Direct conversation resolution result for one target user")
public class DirectConversationBatchItemResponse {

  @Schema(description = "Target user ID", example = "101")
  private Long userId;

  @Schema(
      description = "Direct conversation ID with current user; null if not found",
      example = "67fe2d4bd7f5bd2f0f60d4c2",
      nullable = true)
  private String conversationId;
}
