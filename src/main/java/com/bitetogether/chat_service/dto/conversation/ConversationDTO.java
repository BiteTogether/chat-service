package com.bitetogether.chat_service.dto.conversation;

import com.bitetogether.chat_service.dto.base.BaseResponse;
import com.bitetogether.chat_service.dto.message.ChatMessageDTO;
import com.bitetogether.chat_service.enums.ConversationType;
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
@Schema(description = "Conversation response DTO")
public class ConversationDTO extends BaseResponse {

  @Schema(description = "Conversation ID", example = "conv_abc123")
  private String id;

  @Schema(description = "Conversation type", example = "GROUP")
  private ConversationType type;

  @Schema(description = "Conversation name", example = "Project Team")
  private String name;

  @Schema(description = "Avatar URL", example = "https://example.com/avatar.png")
  private String avatarUrl;

  @Schema(description = "Latest message in the conversation")
  private ChatMessageDTO latestMessage;

  @Schema(description = "List of participants in the conversation")
  private List<ParticipantDTO> participants;

  @Schema(description = "Unread message count for current user", example = "5")
  private Long unreadCount;
}

