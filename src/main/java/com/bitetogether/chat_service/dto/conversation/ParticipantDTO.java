package com.bitetogether.chat_service.dto.conversation;

import com.bitetogether.chat_service.dto.base.BaseResponse;
import com.bitetogether.chat_service.enums.conversation.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Participant information in a conversation")
public class ParticipantDTO extends BaseResponse {

  @Schema(description = "Participant record ID", example = "part_abc123")
  private String id;

  @Schema(description = "Snapshot of participant user profile")
  private ChatUserSnapshotDTO chatUserSnapshot;

  @Schema(description = "Role in the conversation", example = "ADMIN")
  private Role role;

  @Schema(description = "Last read message sequence number", example = "42")
  private Long lastReadMessageSequence;

}
