package com.bitetogether.chat_service.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.chat_service.configuration.redis.RedisProperties;
import com.bitetogether.chat_service.enums.websocket.UserState;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UserStateServiceImplTest {

  @Mock private ReactiveStringRedisTemplate redisTemplate;
  @Mock private ReactiveValueOperations<String, String> valueOperations;

  private UserStateServiceImpl userStateService;

  @BeforeEach
  void setUp() {
    RedisProperties redisProperties = new RedisProperties();
    RedisProperties.UserState userState = new RedisProperties.UserState();
    userState.setTtl(Duration.ofMinutes(5));
    redisProperties.setUserState(userState);

    userStateService =
        new UserStateServiceImpl(redisTemplate, redisProperties, Schedulers.immediate());
  }

  @Test
  void setUserState_WithValidState_SetsInRedis() {
    // Arrange
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.set(eq("user:state:1"), eq("FOREGROUND"), any(Duration.class)))
        .thenReturn(Mono.just(true));

    // Act & Assert
    StepVerifier.create(userStateService.setUserState(1L, UserState.FOREGROUND)).verifyComplete();

    verify(valueOperations).set("user:state:1", "FOREGROUND", Duration.ofMinutes(5));
  }

  @Test
  void markOffline_SetsOfflineState() {
    // Arrange
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.set(eq("user:state:1"), eq("OFFLINE"), any(Duration.class)))
        .thenReturn(Mono.just(true));

    // Act & Assert
    StepVerifier.create(userStateService.markOffline(1L)).verifyComplete();

    verify(valueOperations).set("user:state:1", "OFFLINE", Duration.ofMinutes(5));
  }

  @Test
  void markBackground_SetsBackgroundState() {
    // Arrange
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.set(eq("user:state:1"), eq("BACKGROUND"), any(Duration.class)))
        .thenReturn(Mono.just(true));

    // Act & Assert
    StepVerifier.create(userStateService.markBackground(1L)).verifyComplete();

    verify(valueOperations).set("user:state:1", "BACKGROUND", Duration.ofMinutes(5));
  }
}
