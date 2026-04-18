package com.bitetogether.chat_service.controller;

import static com.bitetogether.chat_service.util.Constants.ApiPaths.USER_STATE;

import com.bitetogether.chat_service.dto.websocket.UpdateUserStateRequest;
import com.bitetogether.chat_service.dto.websocket.UserStateResponse;
import com.bitetogether.chat_service.service.UserStateService;
import com.bitetogether.common.dto.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping(USER_STATE)
@RequiredArgsConstructor
@Tag(
    name = "User State",
    description = "Manage user presence state (foreground/background/offline)")
@SecurityRequirement(name = "bearerAuth")
public class UserStateController {

  private final UserStateService userStateService;

  @PutMapping
  @Operation(
      summary = "Update user presence state",
      description =
          "Update the current user's presence state to FOREGROUND or BACKGROUND. "
              + "This allows the app to explicitly indicate whether the user is actively using the app "
              + "or has it in the background.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "User state updated successfully",
            content = @Content(schema = @Schema(implementation = UserStateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid state value"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  public Mono<UserStateResponse> updateUserState(
      @AuthenticationPrincipal UserContext userContext,
      @Valid @RequestBody UpdateUserStateRequest request) {

    Long userId = userContext.getUserId();

    return userStateService
        .setUserState(userId, request.state())
        .then(Mono.just(new UserStateResponse(userId, request.state())));
  }
}
