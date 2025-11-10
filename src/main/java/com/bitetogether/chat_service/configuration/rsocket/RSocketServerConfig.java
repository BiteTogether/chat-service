package com.bitetogether.chat_service.configuration.rsocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;

@Configuration
@RequiredArgsConstructor
public class RSocketServerConfig {
  private final ObjectMapper objectMapper;

  @Bean
  public RSocketMessageHandler rSocketMessageHandler() {
    RSocketMessageHandler handler = new RSocketMessageHandler();
    handler.setRSocketStrategies(rSocketStrategies());
    return handler;
  }

  @Bean
  public RSocketStrategies rSocketStrategies() {
    return RSocketStrategies.builder()
        .encoders(encoders -> encoders.add(new Jackson2JsonEncoder(objectMapper)))
        .decoders(decoders -> decoders.add(new Jackson2JsonDecoder(objectMapper)))
        .build();
  }
}
