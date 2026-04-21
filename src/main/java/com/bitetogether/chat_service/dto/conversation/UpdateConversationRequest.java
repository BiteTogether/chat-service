package com.bitetogether.chat_service.dto.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request to update an existing conversation")
public class UpdateConversationRequest {

  @Size(max = 100, message = "Name must be at most 100 characters")
  @Schema(description = "New conversation name", example = "Updated Team Name")
  private String name;
}
