package com.bitetogether.chat_service.dto.message;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to update a message")
public class UpdateMessageRequest {

  @NotBlank(message = "Content cannot be blank")
  @Schema(description = "New message content", example = "Updated message content")
  private String content;
}

