package com.bitetogether.chat_service.configuration.rsocket;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rsocket.client")
public class RsocketProperties {
  private String port;
  private String mappingPath;
  private String transport;
}
