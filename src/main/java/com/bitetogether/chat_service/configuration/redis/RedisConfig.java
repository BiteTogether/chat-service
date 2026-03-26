package com.bitetogether.chat_service.configuration.redis;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.SslOptions;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

@Slf4j
@Configuration
public class RedisConfig {

  @Value("${spring.data.redis.ssl.enabled:false}")
  private boolean sslEnabled;

  @Bean
  public LettuceClientConfiguration lettuceClientConfiguration() {
    LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
        LettuceClientConfiguration.builder();

    // Socket options
    SocketOptions socketOptions =
        SocketOptions.builder().connectTimeout(Duration.ofSeconds(10)).build();

    // SSL options for Upstash
    if (sslEnabled) {
      SslOptions sslOptions = SslOptions.builder().build();

      ClientOptions clientOptions =
          ClientOptions.builder().socketOptions(socketOptions).sslOptions(sslOptions).build();

      builder.clientOptions(clientOptions);
      builder.useSsl();

      log.info("Redis SSL/TLS enabled for Upstash connection");
    } else {
      ClientOptions clientOptions = ClientOptions.builder().socketOptions(socketOptions).build();
      builder.clientOptions(clientOptions);
    }

    builder.commandTimeout(Duration.ofSeconds(5));

    return builder.build();
  }

  @Bean
  public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
      ReactiveRedisConnectionFactory factory) {
    log.info(
        "Initializing ReactiveStringRedisTemplate with factory: {}", factory.getClass().getName());
    return new ReactiveStringRedisTemplate(factory);
  }
}
