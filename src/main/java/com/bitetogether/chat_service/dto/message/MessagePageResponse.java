package com.bitetogether.chat_service.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Paginated response for messages")
public class MessagePageResponse {

  @Schema(description = "List of messages")
  private List<ChatMessageDTO> messages;

  @Schema(description = "Cursor for the next page (last message sequence)", example = "42")
  private Long nextCursor;

  @Schema(description = "Whether there are more messages to fetch", example = "true")
  private boolean hasMore;

  @Schema(description = "Total number of messages returned", example = "20")
  private int size;
}
