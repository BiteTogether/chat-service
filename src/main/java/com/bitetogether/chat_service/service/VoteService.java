package com.bitetogether.chat_service.service;

import com.bitetogether.chat_service.dto.vote.CastVoteRequest;
import com.bitetogether.chat_service.dto.vote.CreateVoteSessionRequest;
import com.bitetogether.chat_service.dto.vote.VoteSessionDTO;
import com.bitetogether.chat_service.enums.VoteSessionRealtimeEvent;
import com.bitetogether.chat_service.enums.VoteSessionStatus;
import com.bitetogether.chat_service.event.DomainEventPublisher;
import com.bitetogether.chat_service.exception.ErrorCode;
import com.bitetogether.chat_service.model.Participant;
import com.bitetogether.chat_service.model.VoteSession;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import com.bitetogether.chat_service.repository.VoteSessionRepository;
import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.common.util.ApiResponseUtil;
import com.bitetogether.common.util.ReactiveUserContextUtils;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoteService {
  private static final String USER_ID_NOT_FOUND_MSG = "User ID not found in context";

  VoteSessionRepository voteSessionRepository;
  ParticipantRepository participantRepository;
  DomainEventPublisher domainEventPublisher;

  public Mono<ApiResponseDTO<VoteSessionDTO>> createVoteSession(CreateVoteSessionRequest request) {
    return ReactiveUserContextUtils.getUserIdOrError(USER_ID_NOT_FOUND_MSG)
        .flatMap(
            userId ->
                validateParticipant(request.getConversationId(), userId)
                    .then(buildVoteSession(request, userId))
                    .flatMap(voteSessionRepository::save)
                    .map(this::toDto)
                    .doOnNext(
                        dto ->
                            domainEventPublisher.publishVoteSessionEvent(
                                new VoteSessionRealtimeEvent(
                                    dto.conversationId(), "VOTE_CREATED", dto)))
                    .map(
                        dto ->
                            ApiResponseUtil.buildApiResponse(
                                ApiResponseStatus.CREATED,
                                "Vote session created successfully",
                                dto)));
  }

  public Mono<ApiResponseDTO<VoteSessionDTO>> castVote(
      String voteSessionId, CastVoteRequest request) {
    return ReactiveUserContextUtils.getUserIdOrError(USER_ID_NOT_FOUND_MSG)
        .flatMap(
            userId ->
                voteSessionRepository
                    .findById(voteSessionId)
                    .switchIfEmpty(Mono.error(new AppException(ErrorCode.VOTE_SESSION_NOT_FOUND)))
                    .flatMap(
                        session ->
                            validateParticipant(session.getConversationId(), userId)
                                .thenReturn(session))
                    .flatMap(session -> castVoteInternal(session, userId, request.getOptionId()))
                    .flatMap(voteSessionRepository::save)
                    .map(this::toDto)
                    .doOnNext(
                        dto ->
                            domainEventPublisher.publishVoteSessionEvent(
                                new VoteSessionRealtimeEvent(
                                    dto.conversationId(), "VOTE_CAST", dto)))
                    .map(
                        dto ->
                            ApiResponseUtil.buildApiResponse(
                                ApiResponseStatus.SUCCESS, "Vote cast successfully", dto)));
  }

  public Mono<ApiResponseDTO<VoteSessionDTO>> closeVoteSession(String voteSessionId) {
    return ReactiveUserContextUtils.getUserIdOrError(USER_ID_NOT_FOUND_MSG)
        .flatMap(
            userId ->
                voteSessionRepository
                    .findById(voteSessionId)
                    .switchIfEmpty(Mono.error(new AppException(ErrorCode.VOTE_SESSION_NOT_FOUND)))
                    .flatMap(
                        session ->
                            validateParticipant(session.getConversationId(), userId)
                                .then(closeSessionInternal(session)))
                    .flatMap(voteSessionRepository::save)
                    .map(this::toDto)
                    .doOnNext(
                        dto ->
                            domainEventPublisher.publishVoteSessionEvent(
                                new VoteSessionRealtimeEvent(
                                    dto.conversationId(), "VOTE_CLOSED", dto)))
                    .map(
                        dto ->
                            ApiResponseUtil.buildApiResponse(
                                ApiResponseStatus.SUCCESS,
                                "Vote session closed successfully",
                                dto)));
  }

  public Mono<ApiResponseDTO<VoteSessionDTO>> getVoteSession(String voteSessionId) {
    return ReactiveUserContextUtils.getUserIdOrError(USER_ID_NOT_FOUND_MSG)
        .flatMap(
            userId ->
                voteSessionRepository
                    .findById(voteSessionId)
                    .switchIfEmpty(Mono.error(new AppException(ErrorCode.VOTE_SESSION_NOT_FOUND)))
                    .flatMap(
                        session ->
                            validateParticipant(session.getConversationId(), userId)
                                .thenReturn(session))
                    .map(this::toDto)
                    .map(
                        dto ->
                            ApiResponseUtil.buildApiResponse(
                                ApiResponseStatus.SUCCESS,
                                "Vote session retrieved successfully",
                                dto)));
  }

  public Mono<ApiResponseDTO<List<VoteSessionDTO>>> getConversationVoteSessions(
      String conversationId) {
    return ReactiveUserContextUtils.getUserIdOrError(USER_ID_NOT_FOUND_MSG)
        .flatMap(
            userId ->
                validateParticipant(conversationId, userId)
                    .thenMany(
                        voteSessionRepository.findByConversationIdOrderByCreatedAtDesc(
                            conversationId))
                    .map(this::toDto)
                    .collectList()
                    .map(
                        sessions ->
                            ApiResponseUtil.buildApiResponse(
                                ApiResponseStatus.SUCCESS,
                                "Vote sessions retrieved successfully",
                                sessions)));
  }

  private Mono<VoteSession> buildVoteSession(CreateVoteSessionRequest request, Long userId) {
    VoteSession session = new VoteSession();
    session.setConversationId(request.getConversationId());
    session.setCreatorId(userId);
    session.setStatus(VoteSessionStatus.OPEN);
    session.setOptions(
        request.getOptions().stream()
            .map(
                input -> {
                  VoteSession.VoteOption option = new VoteSession.VoteOption();
                  option.setId(UUID.randomUUID().toString());
                  option.setPlaceId(input.getPlaceId());
                  option.setName(input.getName());
                  option.setAddress(input.getAddress());
                  option.setLat(input.getLat());
                  option.setLng(input.getLng());
                  option.setLabel(input.getName());
                  return option;
                })
            .toList());
    session.setVotes(new HashMap<>());
    return Mono.just(session);
  }

  private Mono<VoteSession> castVoteInternal(VoteSession session, Long userId, String optionId) {
    if (session.getStatus() == VoteSessionStatus.CLOSED) {
      return Mono.error(new AppException(ErrorCode.VOTE_SESSION_CLOSED));
    }

    boolean optionExists =
        session.getOptions().stream().anyMatch(option -> option.getId().equals(optionId));
    if (!optionExists) {
      return Mono.error(new AppException(ErrorCode.VOTE_OPTION_NOT_FOUND));
    }

    if (session.getVotes() == null) {
      session.setVotes(new HashMap<>());
    }
    session.getVotes().put(userId, optionId);
    return Mono.just(session);
  }

  private Mono<VoteSession> closeSessionInternal(VoteSession session) {
    if (session.getStatus() == VoteSessionStatus.CLOSED) {
      return Mono.error(new AppException(ErrorCode.VOTE_SESSION_CLOSED));
    }

    session.setStatus(VoteSessionStatus.CLOSED);
    session.setClosedAt(Instant.now());
    session.setWinnerOptionId(resolveWinnerOptionId(session));
    return Mono.just(session);
  }

  private String resolveWinnerOptionId(VoteSession session) {
    if (session.getVotes() == null || session.getVotes().isEmpty()) {
      return null;
    }

    return session.getVotes().values().stream()
        .collect(Collectors.groupingBy(optionId -> optionId, Collectors.counting()))
        .entrySet()
        .stream()
        .max(Comparator.comparingLong(entry -> entry.getValue()))
        .map(java.util.Map.Entry::getKey)
        .orElse(null);
  }

  private Mono<Void> validateParticipant(String conversationId, Long userId) {
    return participantRepository
        .existsByConversationIdAndUserId(conversationId, userId)
        .flatMap(
            exists ->
                Boolean.TRUE.equals(exists)
                    ? Mono.empty()
                    : Mono.error(new AppException(ErrorCode.NOT_A_PARTICIPANT)));
  }

  private VoteSessionDTO toDto(VoteSession session) {
    return VoteSessionDTO.builder()
        .id(session.getId())
        .conversationId(session.getConversationId())
        .createdBy(session.getCreatorId())
        .status(session.getStatus())
        .options(
            session.getOptions() == null
                ? List.of()
                : session.getOptions().stream()
                    .map(
                        option ->
                            VoteSessionDTO.VoteOptionDTO.builder()
                                .id(option.getId())
                                .placeId(option.getPlaceId())
                                .name(option.getName())
                                .address(option.getAddress())
                                .lat(option.getLat())
                                .lng(option.getLng())
                                .label(option.getLabel())
                                .build())
                    .toList())
        .votes(
            session.getVotes() == null
                ? java.util.Map.of()
                : java.util.Map.copyOf(session.getVotes()))
        .winnerOptionId(session.getWinnerOptionId())
        .closedAt(session.getClosedAt())
        .build();
  }

  public Mono<List<Long>> resolveEligibleParticipantIds(
      String conversationId, String voteSessionId) {
    Mono<List<Long>> fromParticipants =
        participantRepository
            .findByConversationId(conversationId)
            .map(Participant::getUserId)
            .collectList();

    if (voteSessionId == null || voteSessionId.isBlank()) {
      return fromParticipants;
    }

    return voteSessionRepository
        .findById(voteSessionId)
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.VOTE_SESSION_NOT_FOUND)))
        .flatMap(
            voteSession -> {
              if (!conversationId.equals(voteSession.getConversationId())) {
                return Mono.error(new AppException(ErrorCode.VOTE_SESSION_NOT_FOUND));
              }

              List<Long> voterIds =
                  voteSession.getVotes() == null
                      ? List.of()
                      : voteSession.getVotes().keySet().stream().toList();
              return Mono.just(voterIds);
            });
  }

  public Mono<Void> validateVoteSessionCreator(
      String conversationId, String voteSessionId, Long userId) {
    if (voteSessionId == null || voteSessionId.isBlank()) {
      return Mono.error(new AppException(ErrorCode.VOTE_SESSION_NOT_FOUND));
    }

    return voteSessionRepository
        .findById(voteSessionId)
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.VOTE_SESSION_NOT_FOUND)))
        .flatMap(
            voteSession -> {
              if (!conversationId.equals(voteSession.getConversationId())) {
                return Mono.error(new AppException(ErrorCode.VOTE_SESSION_NOT_FOUND));
              }
              if (!userId.equals(voteSession.getCreatorId())) {
                return Mono.error(new AppException(ErrorCode.CONVERSATION_UPDATE_UNAUTHORIZED));
              }
              return Mono.empty();
            });
  }
}
