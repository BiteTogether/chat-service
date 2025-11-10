package com.bitetogether.chat_service.configuration.rsocket;

import io.rsocket.frame.decoder.PayloadDecoder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ClientRsocketConfig {

  @Bean
  public RSocketServerCustomizer rSocketServerCustomizer() {
    return server ->
        server.payloadDecoder(PayloadDecoder.ZERO_COPY).fragment(1024 * 1024); // 1MB fragments
  }
}
