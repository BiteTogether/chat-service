package com.bitetogether.chat_service.controller;

import com.bitetogether.chat_service.dto.bill.BillSessionDTO;
import com.bitetogether.chat_service.dto.bill.CreateBillSessionRequest;
import com.bitetogether.chat_service.dto.bill.MarkBillSharePaidRequest;
import com.bitetogether.chat_service.service.BillService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bills")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Bill", description = "Bill split and settlement management APIs")
public class BillController {
  BillService billService;

  @PostMapping
  @Operation(
      summary = "Create bill session",
      description = "Creates a bill session for vote participants using EQUAL or CUSTOM split.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Bill session created successfully",
            content = @Content(schema = @Schema(implementation = BillSessionDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "No eligible participants or invalid amount",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not a participant of the conversation",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<BillSessionDTO>>> createBillSession(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description =
                  "Bill session request containing total amount, split type, and optional custom splits",
              required = true,
              content = @Content(schema = @Schema(implementation = CreateBillSessionRequest.class)))
          @Valid
          @RequestBody
          CreateBillSessionRequest request) {
    return billService
        .createBillSession(request)
        .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
  }

  @PostMapping("/{billSessionId}/finalize")
  @Operation(
      summary = "Finalize bill session",
      description = "Finalizes draft bill after validating total split integrity.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Bill session finalized successfully",
            content = @Content(schema = @Schema(implementation = BillSessionDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bill session already finalized or split total mismatch",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Bill session not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<BillSessionDTO>>> finalizeBillSession(
      @Parameter(description = "Bill session ID", required = true, example = "bill_abc123")
          @PathVariable
          String billSessionId) {
    return billService.finalizeBillSession(billSessionId).map(ResponseEntity::ok);
  }

  @PostMapping("/{billSessionId}/payments")
  @Operation(
      summary = "Confirm bill payment",
      description =
          "Marks a member share as paid. By default current user confirms self; creator can confirm another member by userId.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Bill payment updated successfully",
            content = @Content(schema = @Schema(implementation = BillSessionDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bill is not finalized or request is invalid",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Bill session or bill share not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<BillSessionDTO>>> markBillSharePaid(
      @Parameter(description = "Bill session ID", required = true, example = "bill_abc123")
          @PathVariable
          String billSessionId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "Payment confirmation request (optional userId for creator override)",
              required = true,
              content = @Content(schema = @Schema(implementation = MarkBillSharePaidRequest.class)))
          @Valid
          @RequestBody
          MarkBillSharePaidRequest request) {
    return billService.markBillSharePaid(billSessionId, request).map(ResponseEntity::ok);
  }

  @GetMapping("/{billSessionId}")
  @Operation(summary = "Get bill session", description = "Retrieves bill session details by ID.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Bill session retrieved successfully",
            content = @Content(schema = @Schema(implementation = BillSessionDTO.class))),
        @ApiResponse(
            responseCode = "404",
            description = "Bill session not found",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<BillSessionDTO>>> getBillSession(
      @Parameter(description = "Bill session ID", required = true, example = "bill_abc123")
          @PathVariable
          String billSessionId) {
    return billService.getBillSession(billSessionId).map(ResponseEntity::ok);
  }

  @GetMapping("/conversation/{conversationId}")
  @Operation(
      summary = "Get conversation bill sessions",
      description = "Lists bill sessions for a conversation, newest first.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Bill sessions retrieved successfully",
            content = @Content(schema = @Schema(implementation = BillSessionDTO.class))),
        @ApiResponse(
            responseCode = "403",
            description = "User is not a participant of the conversation",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
      })
  public Mono<ResponseEntity<ApiResponseDTO<List<BillSessionDTO>>>> getConversationBillSessions(
      @Parameter(description = "Conversation ID", required = true, example = "conv_abc123")
          @PathVariable
          String conversationId) {
    return billService.getConversationBillSessions(conversationId).map(ResponseEntity::ok);
  }
}
