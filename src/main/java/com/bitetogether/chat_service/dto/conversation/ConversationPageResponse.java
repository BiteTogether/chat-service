package com.bitetogether.chat_service.dto.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Paginated response for conversations")
public class ConversationPageResponse {

  @Schema(description = "List of conversations")
  private List<ConversationDTO> conversations;

  @Schema(description = "Cursor for the next page (last conversation ID)", example = "conv_xyz789")
  private String nextCursor;

  @Schema(description = "Whether there are more conversations to fetch", example = "true")
  private boolean hasMore;

  @Schema(description = "Total number of conversations returned", example = "20")
  private int size;
}
