package com.bitetogether.chat_service.dto.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "Request to add participants to an existing conversation")
public class AddParticipantsRequest {

  @NotEmpty(message = "userIds must not be empty")
  @Schema(description = "User IDs to add", example = "[101, 202, 303]")
  private List<Long> userIds;
}
