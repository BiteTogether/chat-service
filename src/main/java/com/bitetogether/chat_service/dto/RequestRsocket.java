package com.bitetogether.chat_service.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor(onConstructor = @__(@JsonCreator))
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestRsocket {
  @JsonProperty("payload")
  Map<String, Object> payload;

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public <T> void setPayload(String key, T value) {
    if (payload == null) {
      payload = new HashMap<>();
    }
    payload.put(key, value);
  }

  public <T> T getPayloadAs(String key, Class<T> type) {
    if (payload == null) return null;
    Object raw = payload.get(key);
    return objectMapper.convertValue(raw, type);
  }
}
