package com.bitetogether.chat_service.integration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import redis.embedded.RedisServer;

@Configuration
@Profile("test")
public class EmbeddedRedisConfig {

  @Value("${spring.data.redis.port:6370}")
  private int redisPort;

  private RedisServer redisServer;

  @PostConstruct
  public void start() throws Exception {
    redisServer = new RedisServer(redisPort);
    redisServer.start();
  }

  @PreDestroy
  public void stop() throws Exception {
    if (redisServer != null && redisServer.isActive()) {
      redisServer.stop();
    }
  }
}
