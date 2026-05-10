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

  @Schema(description = "Recipient user ID to send notification to", example = "1")
  private Long userId;

  @Schema(description = "Notification title", example = "New message from John")
  private String title;

  @Schema(
      description = "Notification message body / content preview",
      example = "Hello, how are you?")
  private String message;

  @Schema(description = "Timestamp when the message was sent")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime sendAt;

  @Schema(description = "Chat context object")
  private ChatObject chatObject;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "Chat context for the notification")
  public static class ChatObject {

    @Schema(description = "Conversation ID", example = "69fc6500ab2f40fb946ef84b")
    private String id;

    @Schema(description = "Conversation name", example = "Group Chat Name")
    private String name;

    @Schema(description = "List of all recipient user IDs in this conversation (excluding sender)")
    private List<Long> recipientUserIds;
  }
}
