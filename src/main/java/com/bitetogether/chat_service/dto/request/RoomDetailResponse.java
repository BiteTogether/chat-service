package com.bitetogether.chat_service.dto.request;

import com.bitetogether.chat_service.dto.user.UserDetailDTO;
import com.bitetogether.chat_service.enums.RoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomDetailResponse {
  @Schema(description = "Unique identifier of the chat room", example = "507f1f77bcf86cd799439011")
  private String id;

  @Schema(description = "Name of the chat room", example = "Project Discussion")
  private String name;

  @Schema(
      description = "URL or path to the room's avatar/profile image",
      example = "https://example.com/avatars/room-avatar.jpg")
  private String avatar;

  @Schema(description = "List of user IDs who are members of this chat room")
  private List<UserDetailDTO> members;

  @Schema(
      description = "Type of chat room (DIRECT for 1-on-1, GROUP for multiple users)",
      example = "GROUP")
  private RoomType roomType;

  @Schema(description = "List of admin user IDs (only applicable for GROUP rooms)")
  private List<Long> adminIds;

  @Schema(description = "ID of the last message sent in this room")
  private String lastMessageId;

  @Schema(description = "Timestamp of the last message in this room")
  private LocalDateTime lastMessageAt;
}
