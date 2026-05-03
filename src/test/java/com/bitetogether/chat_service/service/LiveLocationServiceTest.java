package com.bitetogether.chat_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.chat_service.dto.location.LiveLocationSnapshot;
import com.bitetogether.chat_service.dto.location.LocationPayload;
import com.bitetogether.chat_service.event.DomainEventPublisher;
import com.bitetogether.chat_service.repository.ChatUserSnapshotRepository;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import com.bitetogether.common.exception.AppException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveSetOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LiveLocationServiceTest {

  @Mock private ParticipantRepository participantRepository;
  @Mock private ChatUserSnapshotRepository chatUserSnapshotRepository;
  @Mock private ReactiveStringRedisTemplate redisTemplate;
  @Mock private ReactiveValueOperations<String, String> valueOperations;
  @Mock private ReactiveSetOperations<String, String> setOperations;
  @Mock private DomainEventPublisher domainEventPublisher;

  private LiveLocationService liveLocationService;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    liveLocationService =
        new LiveLocationService(
            participantRepository,
            chatUserSnapshotRepository,
            redisTemplate,
            objectMapper,
            domainEventPublisher);
    when(chatUserSnapshotRepository.findById(any())).thenReturn(Mono.empty());
  }

  @Test
  void handleLocationUpdate_WithValidPayload_PersistsAndPublishesEvent() {
    LocationPayload payload = new LocationPayload(10.77, 106.69, 12.5, 90.0, 1.2, Instant.now());

    when(participantRepository.existsByConversationIdAndUserId("conv_1", 11L))
        .thenReturn(Mono.just(true));
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(valueOperations.setIfAbsent(startsWith("chat:location:throttle:"), eq("1"), any()))
        .thenReturn(Mono.just(true));
    when(valueOperations.set(startsWith("chat:location:conv_1:11"), anyString(), any()))
        .thenReturn(Mono.just(true));
    when(setOperations.add("chat:location:users:conv_1", "11")).thenReturn(Mono.just(1L));

    StepVerifier.create(liveLocationService.handleLocationUpdate("conv_1", 11L, payload, true))
        .verifyComplete();

    verify(domainEventPublisher)
        .publishLocationUpdated(
            org.mockito.ArgumentMatchers.argThat(
                event -> event.conversationId().equals("conv_1") && event.location().sharing()));
  }

  @Test
  void handleLocationUpdate_WhenThrottled_SkipsPersistAndPublish() {
    LocationPayload payload = new LocationPayload(10.77, 106.69, null, null, null, Instant.now());

    when(participantRepository.existsByConversationIdAndUserId("conv_1", 11L))
        .thenReturn(Mono.just(true));
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.setIfAbsent(startsWith("chat:location:throttle:"), eq("1"), any()))
        .thenReturn(Mono.just(false));

    StepVerifier.create(liveLocationService.handleLocationUpdate("conv_1", 11L, payload, true))
        .verifyComplete();

    verify(valueOperations, never()).set(startsWith("chat:location:conv_1:11"), anyString(), any());
    verify(domainEventPublisher, never()).publishLocationUpdated(any());
  }

  @Test
  void handleLocationUpdate_WithStopSharing_RemovesKeysAndPublishesOfflineEvent() {
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 11L))
        .thenReturn(Mono.just(true));
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(redisTemplate.delete("chat:location:conv_1:11")).thenReturn(Mono.just(1L));
    when(setOperations.remove("chat:location:users:conv_1", "11")).thenReturn(Mono.just(1L));

    StepVerifier.create(liveLocationService.handleLocationUpdate("conv_1", 11L, null, false))
        .verifyComplete();

    verify(domainEventPublisher)
        .publishLocationUpdated(
            org.mockito.ArgumentMatchers.argThat(
                event -> event.conversationId().equals("conv_1") && !event.location().sharing()));
  }

  @Test
  void handleLocationUpdate_WithInvalidPayload_ReturnsError() {
    LocationPayload payload = new LocationPayload(120.0, 200.0, null, null, null, Instant.now());

    when(participantRepository.existsByConversationIdAndUserId("conv_1", 11L))
        .thenReturn(Mono.just(true));

    StepVerifier.create(liveLocationService.handleLocationUpdate("conv_1", 11L, payload, true))
        .expectError(AppException.class)
        .verify();
  }

  @Test
  void getActiveLocations_WithMemberSnapshot_ReturnsLocationList() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    LiveLocationSnapshot snapshot =
        new LiveLocationSnapshot(
            "conv_1",
            11L,
            null,
            null,
            10.77,
            106.69,
            null,
            null,
            null,
            Instant.now(),
            true);
    String json = objectMapper.writeValueAsString(snapshot);

    when(participantRepository.existsByConversationIdAndUserId("conv_1", 99L))
        .thenReturn(Mono.just(true));
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(setOperations.members("chat:location:users:conv_1")).thenReturn(Flux.just("11"));
    when(setOperations.remove("chat:location:users:conv_1", "11")).thenReturn(Mono.just(1L));
    when(valueOperations.get("chat:location:conv_1:11")).thenReturn(Mono.just(json));

    StepVerifier.create(liveLocationService.getActiveLocations("conv_1", 99L))
        .assertNext(
            locations -> {
              assertEquals(1, locations.size());
              LiveLocationSnapshot first = locations.getFirst();
              assertEquals("conv_1", first.conversationId());
              assertEquals(11L, first.userId());
            })
        .verifyComplete();
  }

  @Test
  void handleLocationUpdate_WhenNotParticipant_ReturnsError() {
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 11L))
        .thenReturn(Mono.just(false));

    StepVerifier.create(liveLocationService.handleLocationUpdate("conv_1", 11L, null, false))
        .expectError(AppException.class)
        .verify();

    verify(domainEventPublisher, never()).publishLocationUpdated(any());
  }
}
