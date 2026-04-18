package com.bitetogether.chat_service.service;

import static com.bitetogether.chat_service.util.Constants.RedisKeys.USER_STATE_PREFIX;

import com.bitetogether.chat_service.configuration.redis.RedisProperties;
import com.bitetogether.chat_service.enums.websocket.UserState;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Slf4j
@Service
public class UserStateServiceImpl implements UserStateService {

  private final ReactiveStringRedisTemplate redisTemplate;
  private final RedisProperties redisProperties;
  private final Scheduler virtualThreadScheduler;

  public UserStateServiceImpl(
      ReactiveStringRedisTemplate redisTemplate,
      RedisProperties redisProperties,
      @Qualifier("virtualThreadScheduler") Scheduler virtualThreadScheduler) {
    this.redisTemplate = redisTemplate;
    this.redisProperties = redisProperties;
    this.virtualThreadScheduler = virtualThreadScheduler;
  }

  @Override
  public Mono<Void> setUserState(Long userId, UserState state) {
    String key = USER_STATE_PREFIX + userId;
    Duration ttl = redisProperties.getUserState().getTtl();

    log.debug("Setting user {} state to {} with TTL {} on virtual thread", userId, state, ttl);

    return redisTemplate
        .opsForValue()
        .set(key, state.name(), ttl)
        .doOnSuccess(
            success -> {
              if (Boolean.TRUE.equals(success)) {
                log.debug("Successfully set user {} state to {} with TTL {}", userId, state, ttl);
              } else {
                log.warn("Failed to set user {} state to {}", userId, state);
              }
            })
        .doOnError(
            error -> log.error("Failed to set user {} state: {}", userId, error.getMessage()))
        .subscribeOn(
            virtualThreadScheduler) // Run on virtual thread pool for optimal I/O performance
        .then();
  }

  @Override
  public Mono<Void> markOffline(Long userId) {
    return setUserState(userId, UserState.OFFLINE);
  }

  @Override
  public Mono<Void> markBackground(Long userId) {
    return setUserState(userId, UserState.BACKGROUND);
  }
}
