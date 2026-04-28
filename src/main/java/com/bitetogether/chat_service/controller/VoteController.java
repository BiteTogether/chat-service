package com.bitetogether.chat_service.controller;

import com.bitetogether.chat_service.dto.vote.CastVoteRequest;
import com.bitetogether.chat_service.dto.vote.CreateVoteSessionRequest;
import com.bitetogether.chat_service.dto.vote.UpdateVoteSessionRequest;
import com.bitetogether.chat_service.dto.vote.VoteSessionDTO;
import com.bitetogether.chat_service.service.VoteService;
import com.bitetogether.common.dto.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/votes")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Vote", description = "Vote session management APIs")
public class VoteController {
  VoteService voteService;

  @PostMapping
  @Operation(
      summary = "Create vote session",
      description = "Creates a new vote session for selecting a place in a conversation.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Vote session created successfully",
            content = @Content(schema = @Schema(implementation = VoteSessionDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not a participant of the conversation",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<VoteSessionDTO>>> createVoteSession(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Vote session request containing conversation and place options",
              required = true,
              content = @Content(schema = @Schema(implementation = CreateVoteSessionRequest.class)))
          @Valid
          @RequestBody
          CreateVoteSessionRequest request) {
    return voteService
        .createVoteSession(request)
        .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @PostMapping("/{voteSessionId}/cast")
  @Operation(
      summary = "Cast vote",
      description =
          "Casts or updates the current user's vote for an option in an open vote session.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Vote cast successfully",
            content = @Content(schema = @Schema(implementation = VoteSessionDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Vote session is closed or request is invalid",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Vote session or option not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<VoteSessionDTO>>> castVote(
      @Parameter(description = "Vote session ID", required = true, example = "vote_abc123")
          @PathVariable
          String voteSessionId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Vote request with selected option ID",
              required = true,
              content = @Content(schema = @Schema(implementation = CastVoteRequest.class)))
          @Valid
          @RequestBody
          CastVoteRequest request) {
    return voteService.castVote(voteSessionId, request).map(ResponseEntity::ok);
  }

  @PostMapping("/{voteSessionId}/close")
  @Operation(
      summary = "Close vote session",
      description = "Closes an open vote session and computes the winner option based on votes.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Vote session closed successfully",
            content = @Content(schema = @Schema(implementation = VoteSessionDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Vote session is already closed",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Vote session not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<VoteSessionDTO>>> closeVoteSession(
      @Parameter(description = "Vote session ID", required = true, example = "vote_abc123")
          @PathVariable
          String voteSessionId) {
    return voteService.closeVoteSession(voteSessionId).map(ResponseEntity::ok);
  }

  @GetMapping("/{voteSessionId}")
  @Operation(summary = "Get vote session", description = "Retrieves vote session details by ID.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Vote session retrieved successfully",
            content = @Content(schema = @Schema(implementation = VoteSessionDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Vote session not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<VoteSessionDTO>>> getVoteSession(
      @Parameter(description = "Vote session ID", required = true, example = "vote_abc123")
          @PathVariable
          String voteSessionId) {
    return voteService.getVoteSession(voteSessionId).map(ResponseEntity::ok);
  }

  @GetMapping("/conversation/{conversationId}")
  @Operation(
      summary = "Get conversation vote sessions",
      description = "Lists vote sessions for a conversation, newest first.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Vote sessions retrieved successfully",
            content = @Content(schema = @Schema(implementation = VoteSessionDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not a participant of the conversation",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<List<VoteSessionDTO>>>> getConversationVoteSessions(
      @Parameter(description = "Conversation ID", required = true, example = "conv_abc123")
          @PathVariable
          String conversationId) {
    return voteService.getConversationVoteSessions(conversationId).map(ResponseEntity::ok);
  }

  @PatchMapping("/{voteSessionId}")
  @Operation(summary = "Update vote session", description = "Updates vote session name.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Vote session updated successfully",
            content = @Content(schema = @Schema(implementation = VoteSessionDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Only vote creator can update vote session",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Vote session not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<VoteSessionDTO>>> updateVoteSession(
      @Parameter(description = "Vote session ID", required = true, example = "vote_abc123")
          @PathVariable
          String voteSessionId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Vote session update request",
              required = true,
              content = @Content(schema = @Schema(implementation = UpdateVoteSessionRequest.class)))
          @Valid
          @RequestBody
          UpdateVoteSessionRequest request) {
    return voteService
        .updateVoteSessionName(voteSessionId, request.getName())
        .map(ResponseEntity::ok);
  }
}
