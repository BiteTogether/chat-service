package com.bitetogether.chat_service.dto.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Batch direct conversation resolution response")
public class DirectConversationBatchResponse {

  @Schema(description = "Resolution result per target user")
  private List<DirectConversationBatchItemResponse> items;
}
