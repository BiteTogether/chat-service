package com.bitetogether.chat_service.dto.response;

import com.bitetogether.chat_service.dto.UserDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "Response payload containing chat room details")
public class RoomResponse {

  @Schema(description = "Unique identifier of the chat room", example = "507f1f77bcf86cd799439011")
  private String id;

  @Schema(description = "Name of the chat room", example = "Project Discussion")
  private String name;

  @Schema(
      description = "URL or path to the room's avatar/profile image",
      example = "https://example.com/avatars/room-avatar.jpg")
  private String avatar;

  @Schema(description = "List of users who are members of this chat room")
  private List<UserDTO> users;
}
