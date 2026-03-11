package com.bitetogether.chat_service.configuration.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaProperties {
  private String bootstrapServers;
  private Consumer consumer;
  private Topic topic;

  @Data
  public static class Consumer {
    private String groupId;
  }

  @Data
  public static class Topic {
    private String userEvents;
    private String conversationEvents;
  }
}

