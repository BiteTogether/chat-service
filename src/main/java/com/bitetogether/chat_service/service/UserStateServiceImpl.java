package com.bitetogether.chat_service.service;

import static com.bitetogether.chat_service.util.Constants.RedisKeys.USER_STATE_PREFIX;

import com.bitetogether.chat_service.enums.websocket.UserState;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStateServiceImpl implements UserStateService {

  private static final Duration STATE_TTL = Duration.ofMinutes(30);

  private final ReactiveStringRedisTemplate redisTemplate;

  @Override
  public Mono<Void> setUserState(Long userId, UserState state) {
    String key = USER_STATE_PREFIX + userId;
    log.info("Setting user {} state to {}", userId, state);

    return redisTemplate
        .opsForValue()
        .set(key, state.name(), STATE_TTL)
        .doOnSuccess(
            success -> {
              if (Boolean.TRUE.equals(success)) {
                log.debug(
                    "Successfully set user {} state to {} with TTL {}", userId, state, STATE_TTL);
              } else {
                log.warn("Failed to set user {} state to {}", userId, state);
              }
            })
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
