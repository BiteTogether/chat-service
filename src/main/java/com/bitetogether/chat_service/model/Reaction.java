package com.bitetogether.chat_service.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "reactions")
public class Reaction extends BaseModel {
  @Id private String id;

  @Field("message_id")
  private String messageId;

  @Field("user_id")
  private Long userId;

  @Field("emoji")
  private String emoji;
}
