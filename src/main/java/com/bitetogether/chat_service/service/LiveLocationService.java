package com.bitetogether.chat_service.service;

import com.bitetogether.chat_service.dto.location.LiveLocationSnapshot;
import com.bitetogether.chat_service.dto.message.ChatInboundMessage.LocationPayload;
import com.bitetogether.chat_service.enums.LocationUpdatedEvent;
import com.bitetogether.chat_service.event.DomainEventPublisher;
import com.bitetogether.chat_service.exception.ErrorCode;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import com.bitetogether.common.exception.AppException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LiveLocationService {

  static final Duration LOCATION_TTL = Duration.ofSeconds(90);
  static final Duration THROTTLE_WINDOW = Duration.ofSeconds(2);

  ParticipantRepository participantRepository;
  ReactiveStringRedisTemplate redisTemplate;
  ObjectMapper objectMapper;
  DomainEventPublisher domainEventPublisher;

  public Mono<Void> handleLocationUpdate(
      String conversationId, Long userId, LocationPayload payload, Boolean isSharing) {
    return validateMembership(conversationId, userId)
        .then(
            Mono.defer(
                () ->
                    Boolean.FALSE.equals(isSharing)
                        ? stopSharing(conversationId, userId)
                        : publishLocation(conversationId, userId, payload)));
  }

  public Mono<List<LiveLocationSnapshot>> getActiveLocations(
      String conversationId, Long requesterId) {
    return validateMembership(conversationId, requesterId)
        .thenMany(redisTemplate.opsForSet().members(locationUsersKey(conversationId)))
        .flatMap(memberId -> loadLocationSnapshot(conversationId, memberId))
        .collectList();
  }

  private Mono<Void> publishLocation(String conversationId, Long userId, LocationPayload payload) {
    if (payload == null || payload.lat() == null || payload.lng() == null) {
      return Mono.error(new AppException(ErrorCode.INVALID_LOCATION_PAYLOAD));
    }

    if (!isLatitudeValid(payload.lat()) || !isLongitudeValid(payload.lng())) {
      return Mono.error(new AppException(ErrorCode.INVALID_LOCATION_PAYLOAD));
    }

    return checkThrottle(conversationId, userId)
        .flatMap(
            shouldProcess -> {
              if (!shouldProcess) {
                return Mono.empty();
              }

              LiveLocationSnapshot snapshot =
                  new LiveLocationSnapshot(
                      conversationId,
                      userId,
                      payload.lat(),
                      payload.lng(),
                      payload.accuracy(),
                      payload.heading(),
                      payload.speed(),
                      payload.timestamp() != null ? payload.timestamp() : Instant.now(),
                      true);

              return persistSnapshot(snapshot)
                  .doOnSuccess(
                      unused ->
                          domainEventPublisher.publishLocationUpdated(
                              new LocationUpdatedEvent(conversationId, snapshot)));
            });
  }

  private Mono<Void> stopSharing(String conversationId, Long userId) {
    LiveLocationSnapshot snapshot =
        new LiveLocationSnapshot(
            conversationId, userId, null, null, null, null, null, Instant.now(), false);

    return redisTemplate
        .delete(locationKey(conversationId, userId))
        .then(
            redisTemplate
                .opsForSet()
                .remove(locationUsersKey(conversationId), String.valueOf(userId)))
        .then(
            Mono.fromRunnable(
                () ->
                    domainEventPublisher.publishLocationUpdated(
                        new LocationUpdatedEvent(conversationId, snapshot))));
  }

  private Mono<Void> persistSnapshot(LiveLocationSnapshot snapshot) {
    return toJson(snapshot)
        .flatMap(
            json ->
                redisTemplate
                    .opsForValue()
                    .set(
                        locationKey(snapshot.conversationId(), snapshot.userId()),
                        json,
                        LOCATION_TTL))
        .flatMap(
            saved ->
                Boolean.TRUE.equals(saved)
                    ? redisTemplate
                        .opsForSet()
                        .add(
                            locationUsersKey(snapshot.conversationId()),
                            String.valueOf(snapshot.userId()))
                        .then()
                    : Mono.empty());
  }

  private Mono<LiveLocationSnapshot> loadLocationSnapshot(String conversationId, String memberId) {
    Long userId;
    try {
      userId = Long.parseLong(memberId);
    } catch (NumberFormatException e) {
      return redisTemplate
          .opsForSet()
          .remove(locationUsersKey(conversationId), memberId)
          .then(Mono.empty());
    }

    return redisTemplate
        .opsForValue()
        .get(locationKey(conversationId, userId))
        .flatMap(this::fromJson)
        .switchIfEmpty(
            redisTemplate
                .opsForSet()
                .remove(locationUsersKey(conversationId), memberId)
                .then(Mono.empty()));
  }

  private Mono<Boolean> checkThrottle(String conversationId, Long userId) {
    return redisTemplate
        .opsForValue()
        .setIfAbsent(throttleKey(conversationId, userId), "1", THROTTLE_WINDOW)
        .map(Boolean.TRUE::equals)
        .defaultIfEmpty(false);
  }

  private Mono<Void> validateMembership(String conversationId, Long userId) {
    return participantRepository
        .existsByConversationIdAndUserId(conversationId, userId)
        .flatMap(
            exists ->
                Boolean.TRUE.equals(exists)
                    ? Mono.empty()
                    : Mono.error(new AppException(ErrorCode.NOT_A_PARTICIPANT)));
  }

  private Mono<String> toJson(LiveLocationSnapshot snapshot) {
    return Mono.fromCallable(() -> objectMapper.writeValueAsString(snapshot));
  }

  private Mono<LiveLocationSnapshot> fromJson(String json) {
    try {
      return Mono.just(objectMapper.readValue(json, LiveLocationSnapshot.class));
    } catch (JsonProcessingException e) {
      return Mono.empty();
    }
  }

  private static String locationKey(String conversationId, Long userId) {
    return "chat:location:" + conversationId + ":" + userId;
  }

  private static String locationUsersKey(String conversationId) {
    return "chat:location:users:" + conversationId;
  }

  private static String throttleKey(String conversationId, Long userId) {
    return "chat:location:throttle:" + conversationId + ":" + userId;
  }

  private static boolean isLatitudeValid(Double lat) {
    return lat >= -90d && lat <= 90d;
  }

  private static boolean isLongitudeValid(Double lng) {
    return lng >= -180d && lng <= 180d;
  }
}
