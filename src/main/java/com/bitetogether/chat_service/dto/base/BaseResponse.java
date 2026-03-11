package com.bitetogether.chat_service.dto.base;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseResponse {
  @Schema(description = "Timestamp when the entity was created", example = "2024-01-01T12:00:00Z")
  private Instant createdAt;

  @Schema(
      description = "Timestamp when the entity was last updated",
      example = "2024-01-02T15:30:00Z")
  private Instant updatedAt;

  @Schema(description = "Created by user or system", example = "SYSTEM")
  private String createdBy;

  @Schema(description = "Last updated by user or system", example = "SYSTEM")
  private String updatedBy;
}
