package com.bitetogether.chat_service.service;

import com.bitetogether.chat_service.dto.bill.BillSessionDTO;
import com.bitetogether.chat_service.dto.bill.CreateBillSessionRequest;
import com.bitetogether.chat_service.dto.bill.MarkBillSharePaidRequest;
import com.bitetogether.chat_service.enums.bill.BillSessionRealtimeEvent;
import com.bitetogether.chat_service.enums.bill.BillShareStatus;
import com.bitetogether.chat_service.enums.bill.BillSplitType;
import com.bitetogether.chat_service.enums.bill.BillStatus;
import com.bitetogether.chat_service.event.DomainEventPublisher;
import com.bitetogether.chat_service.exception.ErrorCode;
import com.bitetogether.chat_service.model.BillSession;
import com.bitetogether.chat_service.repository.BillSessionRepository;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.common.util.ApiResponseUtil;
import com.bitetogether.common.util.ReactiveUserContextUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BillService {
  private static final String USER_ID_NOT_FOUND_MSG = "User ID not found in context";
  private static final BigDecimal ONE_CENT = new BigDecimal("0.01");

  BillSessionRepository billSessionRepository;
  ParticipantRepository participantRepository;
  VoteService voteService;
  DomainEventPublisher domainEventPublisher;

  public Mono<ApiResponseDTO<BillSessionDTO>> createBillSession(CreateBillSessionRequest request) {
    return ReactiveUserContextUtils.getUserIdOrError(USER_ID_NOT_FOUND_MSG)
        .flatMap(
            userId ->
                validateParticipant(request.getConversationId(), userId)
                    .then(
                        voteService.resolveEligibleParticipantIds(
                            request.getConversationId(), request.getVoteSessionId()))
                    .flatMap(
                        participantIds ->
                            validateSplitPermission(request, userId)
                                .then(buildBillSession(request, userId, participantIds)))
                    .flatMap(
                        billSession ->
                            billSessionRepository
                                .save(billSession)
                                .map(this::toDto)
                                .doOnNext(
                                    dto ->
                                        domainEventPublisher.publishBillSessionEvent(
                                            new BillSessionRealtimeEvent(
                                                dto.conversationId(), "BILL_CREATED", dto)))
                                .map(
                                    dto ->
                                        ApiResponseUtil.buildApiResponse(
                                            ApiResponseStatus.CREATED,
                                            "Bill session created successfully",
                                            dto))));
  }

  private Mono<Void> validateSplitPermission(CreateBillSessionRequest request, Long userId) {
    if (request.getSplitType() != BillSplitType.CUSTOM) {
      return Mono.empty();
    }

    return voteService
        .validateVoteSessionCreator(request.getConversationId(), request.getVoteSessionId(), userId)
        .onErrorMap(
            AppException.class,
            ex ->
                ex.getErrorCode() == ErrorCode.CONVERSATION_UPDATE_UNAUTHORIZED
                    ? new AppException(ErrorCode.BILL_CUSTOM_SPLIT_REQUIRES_VOTE_CREATOR)
                    : ex);
  }

  public Mono<ApiResponseDTO<BillSessionDTO>> finalizeBillSession(String billSessionId) {
    return ReactiveUserContextUtils.getUserIdOrError(USER_ID_NOT_FOUND_MSG)
        .flatMap(
            userId ->
                billSessionRepository
                    .findById(billSessionId)
                    .switchIfEmpty(Mono.error(new AppException(ErrorCode.BILL_SESSION_NOT_FOUND)))
                    .flatMap(
                        session ->
                            validateParticipant(session.getConversationId(), userId)
                                .thenReturn(session))
                    .flatMap(this::finalizeInternal)
                    .flatMap(billSessionRepository::save)
                    .map(this::toDto)
                    .doOnNext(
                        dto ->
                            domainEventPublisher.publishBillSessionEvent(
                                new BillSessionRealtimeEvent(
                                    dto.conversationId(), "BILL_FINALIZED", dto)))
                    .map(
                        dto ->
                            ApiResponseUtil.buildApiResponse(
                                ApiResponseStatus.SUCCESS,
                                "Bill session finalized successfully",
                                dto)));
  }

  public Mono<ApiResponseDTO<BillSessionDTO>> markBillSharePaid(
      String billSessionId, MarkBillSharePaidRequest request) {
    return ReactiveUserContextUtils.getUserIdOrError(USER_ID_NOT_FOUND_MSG)
        .flatMap(
            userId ->
                billSessionRepository
                    .findById(billSessionId)
                    .switchIfEmpty(Mono.error(new AppException(ErrorCode.BILL_SESSION_NOT_FOUND)))
                    .flatMap(
                        session ->
                            validateParticipant(session.getConversationId(), userId)
                                .thenReturn(session))
                    .flatMap(session -> markPaidInternal(session, userId, request.getUserId()))
                    .flatMap(billSessionRepository::save)
                    .map(this::toDto)
                    .doOnNext(
                        dto -> {
                          domainEventPublisher.publishBillSessionEvent(
                              new BillSessionRealtimeEvent(
                                  dto.conversationId(), "BILL_PAYMENT_UPDATED", dto));
                          if (dto.status() == BillStatus.SETTLED) {
                            domainEventPublisher.publishBillSessionEvent(
                                new BillSessionRealtimeEvent(
                                    dto.conversationId(), "BILL_SETTLED", dto));
                          }
                        })
                    .map(
                        dto ->
                            ApiResponseUtil.buildApiResponse(
                                ApiResponseStatus.SUCCESS,
                                "Bill payment updated successfully",
                                dto)));
  }

  public Mono<ApiResponseDTO<BillSessionDTO>> getBillSession(String billSessionId) {
    return ReactiveUserContextUtils.getUserIdOrError(USER_ID_NOT_FOUND_MSG)
        .flatMap(
            userId ->
                billSessionRepository
                    .findById(billSessionId)
                    .switchIfEmpty(Mono.error(new AppException(ErrorCode.BILL_SESSION_NOT_FOUND)))
                    .flatMap(
                        session ->
                            validateParticipant(session.getConversationId(), userId)
                                .thenReturn(session))
                    .map(this::toDto)
                    .map(
                        dto ->
                            ApiResponseUtil.buildApiResponse(
                                ApiResponseStatus.SUCCESS,
                                "Bill session retrieved successfully",
                                dto)));
  }

  public Mono<ApiResponseDTO<List<BillSessionDTO>>> getConversationBillSessions(
      String conversationId) {
    return ReactiveUserContextUtils.getUserIdOrError(USER_ID_NOT_FOUND_MSG)
        .flatMap(
            userId ->
                validateParticipant(conversationId, userId)
                    .thenMany(
                        billSessionRepository.findByConversationIdOrderByCreatedAtDesc(
                            conversationId))
                    .map(this::toDto)
                    .collectList()
                    .map(
                        sessions ->
                            ApiResponseUtil.buildApiResponse(
                                ApiResponseStatus.SUCCESS,
                                "Bill sessions retrieved successfully",
                                sessions)));
  }

  private Mono<BillSession> buildBillSession(
      CreateBillSessionRequest request, Long createdBy, List<Long> participantIds) {
    if (participantIds == null || participantIds.isEmpty()) {
      return Mono.error(new AppException(ErrorCode.BILL_PARTICIPANTS_EMPTY));
    }

    List<Long> sortedParticipantIds =
        participantIds.stream().sorted(Comparator.naturalOrder()).toList();
    BigDecimal totalAmount = request.getTotalAmount().setScale(2, RoundingMode.HALF_UP);
    List<BillSession.BillShare> shares =
        request.getSplitType() == BillSplitType.CUSTOM
            ? splitCustom(totalAmount, sortedParticipantIds, request.getCustomSplits())
            : splitEqual(totalAmount, sortedParticipantIds);

    BillSession session = new BillSession();
    session.setConversationId(request.getConversationId());
    session.setVoteSessionId(request.getVoteSessionId());
    session.setCreatorId(createdBy);
    session.setCurrency(request.getCurrency());
    session.setTotalAmount(totalAmount);
    session.setStatus(BillStatus.DRAFT);
    session.setSplitType(request.getSplitType());
    session.setShares(shares);
    return Mono.just(session);
  }

  private List<BillSession.BillShare> splitEqual(BigDecimal total, List<Long> participantIds) {
    int count = participantIds.size();
    BigDecimal[] division = total.divideAndRemainder(BigDecimal.valueOf(count));
    BigDecimal base = division[0].setScale(2, RoundingMode.DOWN);
    BigDecimal remainder = total.subtract(base.multiply(BigDecimal.valueOf(count)));
    int pennies = remainder.multiply(BigDecimal.valueOf(100)).intValue();

    List<BillSession.BillShare> shares = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      BillSession.BillShare share = new BillSession.BillShare();
      share.setUserId(participantIds.get(i));
      share.setAmount(base.add(i < pennies ? ONE_CENT : BigDecimal.ZERO));
      share.setPaidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
      share.setPaid(Boolean.FALSE);
      share.setStatus(BillShareStatus.UNPAID);
      shares.add(share);
    }
    return shares;
  }

  private List<BillSession.BillShare> splitCustom(
      BigDecimal total,
      List<Long> participantIds,
      List<CreateBillSessionRequest.CustomSplitItem> customSplits) {
    if (customSplits == null || customSplits.isEmpty()) {
      throw new AppException(ErrorCode.BILL_TOTAL_MISMATCH);
    }

    Set<Long> participants = Set.copyOf(participantIds);
    Map<Long, BigDecimal> amountByUser = new HashMap<>();

    for (CreateBillSessionRequest.CustomSplitItem splitItem : customSplits) {
      if (!participants.contains(splitItem.getUserId())) {
        throw new AppException(ErrorCode.BILL_SHARE_NOT_FOUND);
      }
      amountByUser.put(
          splitItem.getUserId(), splitItem.getAmount().setScale(2, RoundingMode.HALF_UP));
    }

    if (amountByUser.size() != participantIds.size()) {
      throw new AppException(ErrorCode.BILL_TOTAL_MISMATCH);
    }

    BigDecimal sum = amountByUser.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    if (sum.compareTo(total.setScale(2, RoundingMode.HALF_UP)) != 0) {
      throw new AppException(ErrorCode.BILL_TOTAL_MISMATCH);
    }

    return participantIds.stream()
        .map(
            userId -> {
              BillSession.BillShare share = new BillSession.BillShare();
              share.setUserId(userId);
              share.setAmount(amountByUser.get(userId));
              share.setPaidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
              share.setPaid(Boolean.FALSE);
              share.setStatus(BillShareStatus.UNPAID);
              return share;
            })
        .toList();
  }

  private Mono<BillSession> finalizeInternal(BillSession session) {
    if (session.getStatus() != BillStatus.DRAFT) {
      return Mono.error(new AppException(ErrorCode.BILL_SESSION_ALREADY_FINALIZED));
    }

    BigDecimal sumShares =
        session.getShares().stream()
            .map(BillSession.BillShare::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

    if (sumShares.compareTo(session.getTotalAmount().setScale(2, RoundingMode.HALF_UP)) != 0) {
      return Mono.error(new AppException(ErrorCode.BILL_TOTAL_MISMATCH));
    }

    session.setStatus(BillStatus.FINALIZED);
    return Mono.just(session);
  }

  private Mono<BillSession> markPaidInternal(
      BillSession session, Long requesterId, Long explicitUserId) {
    if (session.getStatus() == BillStatus.DRAFT) {
      return Mono.error(new AppException(ErrorCode.BILL_SESSION_NOT_FINALIZED));
    }

    Long targetUserId = explicitUserId != null ? explicitUserId : requesterId;
    if (explicitUserId != null && !requesterId.equals(session.getCreatorId())) {
      return Mono.error(new AppException(ErrorCode.CONVERSATION_UPDATE_UNAUTHORIZED));
    }

    BillSession.BillShare target =
        session.getShares().stream()
            .filter(share -> share.getUserId().equals(targetUserId))
            .findFirst()
            .orElseThrow(() -> new AppException(ErrorCode.BILL_SHARE_NOT_FOUND));

    target.setPaidAmount(target.getAmount().setScale(2, RoundingMode.HALF_UP));
    target.setPaid(Boolean.TRUE);
    target.setStatus(BillShareStatus.PAID);

    boolean allPaid =
        session.getShares().stream().allMatch(share -> share.getStatus() == BillShareStatus.PAID);
    if (allPaid) {
      session.setStatus(BillStatus.SETTLED);
    }

    return Mono.just(session);
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

  private BillSessionDTO toDto(BillSession session) {
    return BillSessionDTO.builder()
        .id(session.getId())
        .conversationId(session.getConversationId())
        .voteSessionId(session.getVoteSessionId())
        .createdBy(session.getCreatorId())
        .currency(session.getCurrency())
        .totalAmount(session.getTotalAmount())
        .status(session.getStatus())
        .splitType(session.getSplitType())
        .shares(
            session.getShares() == null
                ? List.of()
                : session.getShares().stream()
                    .map(
                        share ->
                            BillSessionDTO.BillShareDTO.builder()
                                .userId(share.getUserId())
                                .amount(share.getAmount())
                                .paidAmount(share.getPaidAmount())
                                .paid(share.getPaid())
                                .status(share.getStatus())
                                .build())
                    .toList())
        .build();
  }
}
