package com.bitetogether.chat_service.dto.conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Batch add participants response")
public class AddParticipantsResponse {

  @Schema(description = "Participants that were newly added")
  private List<ParticipantDTO> addedParticipants;

  @Schema(description = "User IDs skipped because they are already participants", example = "[101]")
  private List<Long> skippedUserIds;
}
