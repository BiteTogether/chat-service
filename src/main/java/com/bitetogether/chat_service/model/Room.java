package com.bitetogether.chat_service.model;

import java.util.List;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "rooms")
public class Room extends BaseModel {
  @Id private String id;

  @Field("name")
  private String name;

  @Field("avatar")
  private String avatar;

  @Field("user_ids")
  private List<String> userIds;
}
