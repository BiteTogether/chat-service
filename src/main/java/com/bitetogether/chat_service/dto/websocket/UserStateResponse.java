package com.bitetogether.chat_service.dto.websocket;

import com.bitetogether.chat_service.enums.websocket.UserState;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing user presence state")
public record UserStateResponse(
    @Schema(description = "The user ID", example = "123") Long userId,
    @Schema(description = "The current user state", example = "FOREGROUND") UserState state) {}
