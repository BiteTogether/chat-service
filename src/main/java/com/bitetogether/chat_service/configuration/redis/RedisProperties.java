package com.bitetogether.chat_service.configuration.redis;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisProperties {

  private UserState userState = new UserState();

  @Data
  public static class UserState {
    private Duration ttl;
  }
}
