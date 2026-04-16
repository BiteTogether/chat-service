package com.bitetogether.chat_service.model;

import com.bitetogether.chat_service.enums.conversation.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "participants")
@Data
@EqualsAndHashCode(callSuper = true)
public class Participant extends BaseModel {
  @Id private String id;

  @Field("user_id")
  private Long userId;

  @Field("conversation_id")
  private String conversationId;

  @Field("role")
  private Role role;

  @Field("last_read_sequence")
  private Long lastReadMessageSequence;
}
