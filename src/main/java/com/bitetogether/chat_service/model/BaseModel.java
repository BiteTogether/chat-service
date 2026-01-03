package com.bitetogether.chat_service.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
public abstract class BaseModel {
  @CreatedDate
  @Field("created_at")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "UTC")
  private Instant createdAt;

  @LastModifiedDate
  @Field("updated_at")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "UTC")
  private Instant updatedAt;

  @CreatedBy
  @Field("created_by")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "UTC")
  private String createdBy;

  @LastModifiedBy
  @Field("updated_by")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "UTC")
  private String updatedBy;
}
