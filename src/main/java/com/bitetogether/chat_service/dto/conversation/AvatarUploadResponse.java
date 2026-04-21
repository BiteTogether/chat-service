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
@Schema(description = "Response containing the uploaded avatar URL")
public class AvatarUploadResponse {

  @Schema(
      description = "Public URL of the uploaded avatar",
      example =
          "https://firebasestorage.googleapis.com/v0/b/bucket/o/avatars%2Fconversation_abc123%2Fuuid.jpg?alt=media")
  private String avatarUrl;
}
