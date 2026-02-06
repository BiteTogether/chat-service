package com.bitetogether.chat_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class SequenceService {

    private final ReactiveStringRedisTemplate redisTemplate;

    private static final String KEY_PATTERN = "chat:conv:%s:seq";

    public Mono<Long> nextSeq(String conversationId) {
        String key = KEY_PATTERN.formatted(conversationId);
        return redisTemplate.opsForValue()
                .increment(key)
                .switchIfEmpty(Mono.error(new IllegalStateException("Cannot generate seq")));
    }
}
