package com.bitetogether.chat_service.model;

import com.bitetogether.chat_service.enums.RoomType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@EqualsAndHashCode(callSuper = true)
@Document(collection = "rooms")
public class Room extends BaseModel {
  @Id private String id;

  @Field("name")
  private String name;

  @Field("avatar")
  private String avatar;

  @Field("user_ids")
  private List<Long> userIds;

  @Field("room_type")
  private RoomType roomType; // DIRECT, GROUP

  @Field("admin_ids")
  private List<Long> adminIds;

  @Field("last_message_id")
  private String lastMessageId;

  @Field("last_message_at")
  private LocalDateTime lastMessageAt;
}
