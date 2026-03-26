package com.bitetogether.chat_service.service;

import com.bitetogether.chat_service.model.Message;
import com.bitetogether.chat_service.repository.MessageRepository;
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
  MessageRepository messageRepository;

  private static final String KEY_PATTERN = "chat:conv:%s:seq";

  public Mono<Long> nextSeq(String conversationId) {
    String key = KEY_PATTERN.formatted(conversationId);

    // First, increment the Redis counter
    return redisTemplate
        .opsForValue()
        .increment(key)
        .switchIfEmpty(Mono.error(new IllegalStateException("Cannot generate seq")))
        .flatMap(
            redisSeq ->
                // For normal, already-initialized conversations where Redis is ahead of or
                // aligned with MongoDB, we can return quickly.
                // If Redis looks too low (e.g., after Redis reset), recover using MongoDB.
                recoverIfStaleOrReturn(conversationId, key, redisSeq));
  }

  private Mono<Long> recoverIfStaleOrReturn(String conversationId, String key, Long redisSeq) {
    return messageRepository
        .findTopByConversationIdOrderBySequenceDesc(conversationId)
        .defaultIfEmpty(Message.builder().sequence(0L).build())
        .flatMap(
            lastMessage -> {
              long dbMaxSeq = lastMessage.getSequence() == null ? 0L : lastMessage.getSequence();

              // Case 1: No messages yet in DB and Redis returned 1 -> normal bootstrap
              if (dbMaxSeq == 0L && redisSeq == 1L) {
                return Mono.just(redisSeq);
              }

              // Case 2: Redis is already at or ahead of DB max + 1 -> accept redisSeq
              if (redisSeq >= dbMaxSeq + 1) {
                return Mono.just(redisSeq);
              }

              // Case 3: Redis is behind DB (e.g. after Redis reset or data loss)
              // We realign by setting Redis to dbMaxSeq and then incrementing once to get
              // a fresh, unique sequence number that is >= dbMaxSeq + 1. Using INCR
              // ensures that concurrent callers are serialized at Redis and receive
              // distinct values.
              return redisTemplate
                  .opsForValue()
                  .set(key, String.valueOf(dbMaxSeq))
                  .then(redisTemplate.opsForValue().increment(key))
                  .switchIfEmpty(Mono.error(new IllegalStateException("Cannot recover seq")));
            });
  }
}
