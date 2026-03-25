package com.bitetogether.chat_service.model;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "chat_user_snapshots")
@Data
@EqualsAndHashCode(callSuper = true)
public class ChatUserSnapshot extends BaseModel {
  @Id
  @Field("user_id")
  private Long userId; // userId is now the primary key

  @Field("username")
  private String username;

  @Field("full_name")
  private String fullName;

  @Field("phone_number")
  private String phoneNumber;

  @Field("avatar")
  private String avatar;

  @Field("version")
  private Long version; // For optimistic locking

  @Field("last_synced_at")
  private LocalDateTime lastSyncedAt;
}
