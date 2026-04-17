package com.bitetogether.chat_service.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Event published to Kafka when a new message is created. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Message notification event for Kafka")
public class MessageNotificationEvent {

  @Schema(description = "Event type", example = "MESSAGE_CREATED")
  private String eventType = "MESSAGE_CREATED";

  @Schema(description = "Conversation ID", example = "conv_xyz789")
  private String conversationId;

  @Schema(description = "Sender user ID", example = "1")
  private Long senderId;

  @Schema(description = "Message content preview for notification", example = "Hello, world!")
  private String contentPreview;

  @Schema(description = "List of recipient user IDs to send notifications to")
  private List<Long> recipientUserIds;

  @Schema(description = "Timestamp when the message was created")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime createdAt;
}
