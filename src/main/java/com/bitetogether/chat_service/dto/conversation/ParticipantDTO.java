package com.bitetogether.chat_service.dto.conversation;

import com.bitetogether.chat_service.dto.base.BaseResponse;
import com.bitetogether.chat_service.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Participant information in a conversation")
public class ParticipantDTO extends BaseResponse {

  @Schema(description = "Participant record ID", example = "part_abc123")
  private String id;

  @Schema(description = "User ID of the participant", example = "1")
  private Long userId;

  @Schema(description = "Username of the participant", example = "john_doe")
  private String username;

  @Schema(description = "Avatar URL of the participant", example = "https://example.com/user-avatar.png")
  private String avatarUrl;

  @Schema(description = "Role in the conversation", example = "ADMIN")
  private Role role;

  @Schema(description = "Last read message sequence number", example = "42")
  private Long lastReadMessageSequence;

  @Schema(description = "Joined timestamp")
  private LocalDateTime joinedAt;
}

