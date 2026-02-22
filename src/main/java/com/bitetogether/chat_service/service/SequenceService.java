package com.bitetogether.chat_service.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SequenceService {

  ReactiveStringRedisTemplate redisTemplate;

  private static final String KEY_PATTERN = "chat:conv:%s:seq";

  public Mono<Long> nextSeq(String conversationId) {
    String key = KEY_PATTERN.formatted(conversationId);
    return redisTemplate
        .opsForValue()
        .increment(key)
        .switchIfEmpty(Mono.error(new IllegalStateException("Cannot generate seq")));
  }
}
