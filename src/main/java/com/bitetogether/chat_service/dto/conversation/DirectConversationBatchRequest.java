package com.bitetogether.chat_service.dto.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "Request to resolve direct conversation IDs for multiple users")
public class DirectConversationBatchRequest {

  @NotEmpty(message = "userIds must not be empty")
  @Schema(description = "Target user IDs", example = "[101, 202, 303]")
  private List<Long> userIds;
}
