package com.bitetogether.chat_service.model;

import com.bitetogether.chat_service.enums.ConversationType;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "conversations")
@Data
public class Conversation extends BaseModel {
  @Id private String id;

  @Field("type")
  private ConversationType type;

  @Field("name")
  private String name;

  @Field("avatar_url")
  private String avatarUrl;

  @Field("last_message_id")
  private String lastMessageId;

  @Field("last_message_time")
  private LocalDateTime lastMessageTime;
}
