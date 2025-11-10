package com.bitetogether.chat_service.model;

import com.bitetogether.chat_service.enums.MessageType;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "messages")
public class Message extends BaseModel {
  @Id private String id;

  @Field("room_id")
  private String roomId;

  @Field("sender_id")
  private String senderId;

  @Field("content")
  private String content;

  @Field("type")
  private MessageType type;
}
