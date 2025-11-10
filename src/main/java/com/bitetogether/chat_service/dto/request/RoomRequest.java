package com.bitetogether.chat_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "Request payload for creating or updating a chat room")
public class RoomRequest {

  @Schema(description = "Name of the chat room", example = "Project Discussion", required = true)
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
}
