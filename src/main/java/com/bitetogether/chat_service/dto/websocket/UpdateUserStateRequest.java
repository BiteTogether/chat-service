package com.bitetogether.chat_service.dto.websocket;

import com.bitetogether.chat_service.enums.websocket.UserState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update user presence state")
public record UpdateUserStateRequest(
    @NotNull @Schema(description = "The new user state", example = "FOREGROUND") UserState state) {}
