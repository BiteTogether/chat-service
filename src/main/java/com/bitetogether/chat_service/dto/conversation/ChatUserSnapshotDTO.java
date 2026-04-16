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
@Schema(description = "Chat user snapshot information")
public class ChatUserSnapshotDTO {

  @Schema(description = "User ID of the participant", example = "1")
  private Long userId;

  @Schema(description = "Username of the participant", example = "john_doe")
  private String username;

  @Schema(description = "Full name of the participant", example = "John Doe")
  private String fullName;

  @Schema(description = "Phone number of the participant", example = "+84901234567")
  private String phoneNumber;

  @Schema(
      description = "Avatar URL of the participant",
      example = "https://example.com/user-avatar.png")
  private String avatar;
}
