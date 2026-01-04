package com.bitetogether.chat_service.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserDTO {
  @Schema(description = "ID of the user", example = "12345")
  private Long id;

  @Schema(description = "Username of the user", example = "john_doe")
  private String username;

  @Schema(description = "Email of the user", example = "john@gmail.com")
  private String email;

  @Schema(description = "Full name of the user", example = "John Doe")
  private String fullName;

  @Schema(description = "Phone number of the user", example = "1234567890")
  private String phoneNumber;

  @Schema(description = "Avatar URL of the user", example = "http://example.com/avatar.jpg")
  private String avatar;

  @Schema(description = "Role of the user", example = "USER")
  private String role;

  @Schema(description = "Account creation timestamp", example = "2023-10-01T12:34:56")
  private LocalDateTime createdAt;

  @Schema(description = "Last account update timestamp", example = "2023-10-10T15:20:30")
  private LocalDateTime updatedAt;
}
