package com.bitetogether.chat_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SenderInfo {
    @Schema(description = "ID of the user", example = "12345")
    private Long id;

    @Schema(description = "Username of the user", example = "john_doe")
    private String username;

    @Schema(description = "Full name of the user", example = "John Doe")
    private String fullName;

    @Schema(description = "Avatar URL of the user", example = "http://example.com/avatar.jpg")
    private String avatar;
}
