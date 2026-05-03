package com.bitetogether.chat_service.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bitetogether.chat_service.model.Message;
import com.bitetogether.chat_service.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SequenceServiceTest {

  @Mock private ReactiveStringRedisTemplate redisTemplate;
  @Mock private ReactiveValueOperations<String, String> valueOperations;
  @Mock private MessageRepository messageRepository;

  private SequenceService sequenceService;

  @BeforeEach
  void setUp() {
    sequenceService = new SequenceService(redisTemplate, messageRepository);
  }

  @Test
  void nextSeq_WhenRedisAhead_ReturnsRedisValue() {
    // Arrange
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment("chat:conv:conv_1:seq")).thenReturn(Mono.just(5L));
    when(messageRepository.findTopByConversationIdOrderBySequenceDesc("conv_1"))
        .thenReturn(Mono.just(Message.builder().sequence(4L).build()));

    // Act & Assert
    StepVerifier.create(sequenceService.nextSeq("conv_1")).expectNext(5L).verifyComplete();
  }

  @Test
  void nextSeq_WhenFirstMessage_ReturnsOne() {
    // Arrange
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment("chat:conv:conv_1:seq")).thenReturn(Mono.just(1L));
    when(messageRepository.findTopByConversationIdOrderBySequenceDesc("conv_1"))
        .thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(sequenceService.nextSeq("conv_1")).expectNext(1L).verifyComplete();
  }

  @Test
  void nextSeq_WhenRedisBehindDb_RecoversFromDb() {
    // Arrange
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.increment("chat:conv:conv_1:seq"))
        .thenReturn(Mono.just(2L))
        .thenReturn(Mono.just(11L));
    when(messageRepository.findTopByConversationIdOrderBySequenceDesc("conv_1"))
        .thenReturn(Mono.just(Message.builder().sequence(10L).build()));
    when(valueOperations.set(eq("chat:conv:conv_1:seq"), eq("10"))).thenReturn(Mono.just(true));

    // Act & Assert
    StepVerifier.create(sequenceService.nextSeq("conv_1")).expectNext(11L).verifyComplete();
  }
}
