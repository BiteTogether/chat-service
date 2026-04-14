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
@Schema(description = "Direct conversation ID response")
public class DirectConversationIdResponse {

  @Schema(description = "Direct conversation ID", example = "67fe2d4bd7f5bd2f0f60d4c2")
  private String conversationId;
}
