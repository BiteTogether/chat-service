package com.bitetogether.chat_service.dto.conversation;

import com.bitetogether.chat_service.enums.ConversationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Data;

@Data
@Schema(description = "Request to create a new conversation")
public class CreateConversationRequest {

  @NotNull(message = "Conversation type is required")
  @Schema(description = "Type of conversation", example = "GROUP")
  private ConversationType type;

  @Size(max = 100, message = "Name must be at most 100 characters")
  @Schema(description = "Conversation name (required for GROUP, optional for DIRECT)", example = "Project Team")
  private String name;

  @Schema(description = "Avatar URL for the conversation", example = "https://example.com/avatar.png")
  private String avatarUrl;

  @NotEmpty(message = "At least one participant is required")
  @Schema(description = "Set of user IDs to add as participants", example = "[1, 2, 3]")
  private Set<Long> participantIds;
}

