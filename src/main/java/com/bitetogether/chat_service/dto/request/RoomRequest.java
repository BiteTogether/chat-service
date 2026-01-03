package com.bitetogether.chat_service.dto.request;

import com.bitetogether.chat_service.enums.RoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Request payload for creating or updating a chat room")
public class RoomRequest {

  @Schema(
      description = "Name of the chat room (required for GROUP, optional for DIRECT)",
      example = "Project Discussion")
  private String name;

  @Schema(
      description = "URL or path to the room's avatar/profile image",
      example = "https://example.com/avatars/room-avatar.jpg")
  private String avatar;

  @Schema(
      description = "List of user IDs who are members of this room",
      example = "[123, 456, 789]")
  private List<Long> userIds;

  @Schema(
      description = "Type of chat room (DIRECT for 1-on-1, GROUP for multiple users)",
      example = "GROUP")
  private RoomType roomType;

  @Schema(
      description = "List of user IDs who are admins of this room (only for GROUP rooms)",
      example = "[123]")
  private List<Long> adminIds;
}
