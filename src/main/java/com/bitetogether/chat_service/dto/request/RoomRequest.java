package com.bitetogether.chat_service.dto.request;

import com.bitetogether.chat_service.enums.RoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
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
      example = "[\"user123\", \"user456\", \"user789\"]",
      required = true)
  private List<String> userIds;

  @Schema(
      description = "Type of chat room (DIRECT for 1-on-1, GROUP for multiple users)",
      example = "GROUP",
      required = true)
  private RoomType roomType;

  @Schema(
      description = "List of user IDs who are admins of this room (only for GROUP rooms)",
      example = "[\"user123\"]")
  private List<String> adminIds;
}
