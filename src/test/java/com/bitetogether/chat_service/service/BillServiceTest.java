package com.bitetogether.chat_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.chat_service.dto.bill.BillSessionDTO;
import com.bitetogether.chat_service.dto.bill.CreateBillSessionRequest;
import com.bitetogether.chat_service.dto.bill.MarkBillSharePaidRequest;
import com.bitetogether.chat_service.enums.bill.BillShareStatus;
import com.bitetogether.chat_service.enums.bill.BillSplitType;
import com.bitetogether.chat_service.enums.bill.BillStatus;
import com.bitetogether.chat_service.event.DomainEventPublisher;
import com.bitetogether.chat_service.exception.ErrorCode;
import com.bitetogether.chat_service.mapper.BillMapper;
import com.bitetogether.chat_service.model.BillSession;
import com.bitetogether.chat_service.repository.BillSessionRepository;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import com.bitetogether.common.dto.UserContext;
import com.bitetogether.common.exception.AppException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

  @Mock private BillSessionRepository billSessionRepository;
  @Mock private ParticipantRepository participantRepository;
  @Mock private VoteService voteService;
  @Mock private DomainEventPublisher domainEventPublisher;
  @Mock private BillMapper billMapper;

  private BillService billService;

  private static final UserContext USER_CONTEXT = new UserContext(1L, "USER", "a@b.com", "user1");

  @BeforeEach
  void setUp() {
    billService =
        new BillService(
            billSessionRepository,
            participantRepository,
            voteService,
            domainEventPublisher,
            billMapper);
  }

  private <T> Mono<T> withUser(Mono<T> mono) {
    return mono.contextWrite(ctx -> ctx.put("USER_CONTEXT", USER_CONTEXT));
  }

  @Test
  void createBillSession_WithEqualSplit_CreatesSuccessfully() {
    // Arrange
    CreateBillSessionRequest request = new CreateBillSessionRequest();
    request.setConversationId("conv_1");
    request.setVoteSessionId("vote_1");
    request.setCurrency("USD");
    request.setTotalAmount(new BigDecimal("10.00"));
    request.setSplitType(BillSplitType.EQUAL);

    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(voteService.resolveVoteParticipantIds("conv_1", "vote_1"))
        .thenReturn(Mono.just(List.of(1L, 2L)));
    when(billSessionRepository.save(any(BillSession.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    BillSessionDTO dto =
        new BillSessionDTO(
            "bill_1",
            "conv_1",
            "vote_1",
            1L,
            "USD",
            new BigDecimal("10.00"),
            BillStatus.DRAFT,
            BillSplitType.EQUAL,
            List.of());
    when(billMapper.toDto(any(BillSession.class))).thenReturn(dto);

    // Act & Assert
    StepVerifier.create(withUser(billService.createBillSession(request)))
        .assertNext(
            response -> {
              assertNotNull(response.getData());
              assertEquals("conv_1", response.getData().conversationId());
            })
        .verifyComplete();

    verify(domainEventPublisher).publishBillSessionEvent(any());
  }

  @Test
  void createBillSession_WithEmptyParticipants_ReturnsError() {
    // Arrange
    CreateBillSessionRequest request = new CreateBillSessionRequest();
    request.setConversationId("conv_1");
    request.setVoteSessionId("vote_1");
    request.setCurrency("USD");
    request.setTotalAmount(new BigDecimal("10.00"));
    request.setSplitType(BillSplitType.EQUAL);

    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(voteService.resolveVoteParticipantIds("conv_1", "vote_1"))
        .thenReturn(Mono.just(List.of()));

    // Act & Assert
    StepVerifier.create(withUser(billService.createBillSession(request)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.BILL_PARTICIPANTS_EMPTY)
        .verify();
  }

  @Test
  void createBillSession_WithCustomSplit_TotalMismatch_ReturnsError() {
    // Arrange
    CreateBillSessionRequest request = new CreateBillSessionRequest();
    request.setConversationId("conv_1");
    request.setVoteSessionId("vote_1");
    request.setCurrency("USD");
    request.setTotalAmount(new BigDecimal("10.00"));
    request.setSplitType(BillSplitType.CUSTOM);

    CreateBillSessionRequest.CustomSplitItem item1 = new CreateBillSessionRequest.CustomSplitItem();
    item1.setUserId(1L);
    item1.setAmount(new BigDecimal("3.00"));
    CreateBillSessionRequest.CustomSplitItem item2 = new CreateBillSessionRequest.CustomSplitItem();
    item2.setUserId(2L);
    item2.setAmount(new BigDecimal("5.00"));
    request.setCustomSplits(List.of(item1, item2));

    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(voteService.resolveVoteParticipantIds("conv_1", "vote_1"))
        .thenReturn(Mono.just(List.of(1L, 2L)));
    when(voteService.validateVoteSessionCreator("conv_1", "vote_1", 1L)).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(withUser(billService.createBillSession(request)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.BILL_TOTAL_MISMATCH)
        .verify();
  }

  @Test
  void createBillSession_WithCustomSplit_ValidTotal_Succeeds() {
    // Arrange
    CreateBillSessionRequest request = new CreateBillSessionRequest();
    request.setConversationId("conv_1");
    request.setVoteSessionId("vote_1");
    request.setCurrency("USD");
    request.setTotalAmount(new BigDecimal("10.00"));
    request.setSplitType(BillSplitType.CUSTOM);

    CreateBillSessionRequest.CustomSplitItem item1 = new CreateBillSessionRequest.CustomSplitItem();
    item1.setUserId(1L);
    item1.setAmount(new BigDecimal("4.00"));
    CreateBillSessionRequest.CustomSplitItem item2 = new CreateBillSessionRequest.CustomSplitItem();
    item2.setUserId(2L);
    item2.setAmount(new BigDecimal("6.00"));
    request.setCustomSplits(List.of(item1, item2));

    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(voteService.resolveVoteParticipantIds("conv_1", "vote_1"))
        .thenReturn(Mono.just(List.of(1L, 2L)));
    when(voteService.validateVoteSessionCreator("conv_1", "vote_1", 1L)).thenReturn(Mono.empty());
    when(billSessionRepository.save(any(BillSession.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    BillSessionDTO dto =
        new BillSessionDTO(
            "bill_1",
            "conv_1",
            "vote_1",
            1L,
            "USD",
            new BigDecimal("10.00"),
            BillStatus.DRAFT,
            BillSplitType.CUSTOM,
            List.of());
    when(billMapper.toDto(any(BillSession.class))).thenReturn(dto);

    // Act & Assert
    StepVerifier.create(withUser(billService.createBillSession(request)))
        .assertNext(response -> assertNotNull(response.getData()))
        .verifyComplete();
  }

  @Test
  void finalizeBillSession_WhenDraft_Finalizes() {
    // Arrange
    BillSession session = new BillSession();
    session.setId("bill_1");
    session.setConversationId("conv_1");
    session.setVoteSessionId("vote_1");
    session.setCreatorId(1L);
    session.setStatus(BillStatus.DRAFT);
    session.setTotalAmount(new BigDecimal("10.00"));

    BillSession.BillShare share1 = new BillSession.BillShare();
    share1.setUserId(1L);
    share1.setAmount(new BigDecimal("5.00"));
    BillSession.BillShare share2 = new BillSession.BillShare();
    share2.setUserId(2L);
    share2.setAmount(new BigDecimal("5.00"));
    session.setShares(List.of(share1, share2));

    when(billSessionRepository.findById("bill_1")).thenReturn(Mono.just(session));
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(voteService.validateVoteSessionCreator("conv_1", "vote_1", 1L)).thenReturn(Mono.empty());
    when(billSessionRepository.save(any(BillSession.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    BillSessionDTO dto =
        new BillSessionDTO(
            "bill_1",
            "conv_1",
            "vote_1",
            1L,
            "USD",
            new BigDecimal("10.00"),
            BillStatus.FINALIZED,
            BillSplitType.EQUAL,
            List.of());
    when(billMapper.toDto(any(BillSession.class))).thenReturn(dto);

    // Act & Assert
    StepVerifier.create(withUser(billService.finalizeBillSession("bill_1")))
        .assertNext(response -> assertEquals(BillStatus.FINALIZED, response.getData().status()))
        .verifyComplete();
  }

  @Test
  void finalizeBillSession_WhenAlreadyFinalized_ReturnsError() {
    // Arrange
    BillSession session = new BillSession();
    session.setId("bill_1");
    session.setConversationId("conv_1");
    session.setVoteSessionId("vote_1");
    session.setCreatorId(1L);
    session.setStatus(BillStatus.FINALIZED);

    when(billSessionRepository.findById("bill_1")).thenReturn(Mono.just(session));
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(voteService.validateVoteSessionCreator("conv_1", "vote_1", 1L)).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(withUser(billService.finalizeBillSession("bill_1")))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode()
                        == ErrorCode.BILL_SESSION_ALREADY_FINALIZED)
        .verify();
  }

  @Test
  void markBillSharePaid_WhenFinalized_MarksAsPaid() {
    // Arrange
    BillSession session = new BillSession();
    session.setId("bill_1");
    session.setConversationId("conv_1");
    session.setCreatorId(1L);
    session.setStatus(BillStatus.FINALIZED);

    BillSession.BillShare share = new BillSession.BillShare();
    share.setUserId(1L);
    share.setAmount(new BigDecimal("5.00"));
    share.setPaidAmount(BigDecimal.ZERO);
    share.setPaid(false);
    share.setStatus(BillShareStatus.UNPAID);
    session.setShares(new ArrayList<>(List.of(share)));

    MarkBillSharePaidRequest request = new MarkBillSharePaidRequest();

    when(billSessionRepository.findById("bill_1")).thenReturn(Mono.just(session));
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(billSessionRepository.save(any(BillSession.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    BillSessionDTO dto =
        new BillSessionDTO(
            "bill_1",
            "conv_1",
            "vote_1",
            1L,
            "USD",
            new BigDecimal("10.00"),
            BillStatus.SETTLED,
            BillSplitType.EQUAL,
            List.of());
    when(billMapper.toDto(any(BillSession.class))).thenReturn(dto);

    // Act & Assert
    StepVerifier.create(withUser(billService.markBillSharePaid("bill_1", request)))
        .assertNext(response -> assertNotNull(response.getData()))
        .verifyComplete();
  }

  @Test
  void markBillSharePaid_WhenDraft_ReturnsNotFinalizedError() {
    // Arrange
    BillSession session = new BillSession();
    session.setId("bill_1");
    session.setConversationId("conv_1");
    session.setCreatorId(1L);
    session.setStatus(BillStatus.DRAFT);

    MarkBillSharePaidRequest request = new MarkBillSharePaidRequest();

    when(billSessionRepository.findById("bill_1")).thenReturn(Mono.just(session));
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));

    // Act & Assert
    StepVerifier.create(withUser(billService.markBillSharePaid("bill_1", request)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.BILL_SESSION_NOT_FINALIZED)
        .verify();
  }

  @Test
  void markBillSharePaid_WhenNotParticipant_ReturnsError() {
    // Arrange
    BillSession session = new BillSession();
    session.setId("bill_1");
    session.setConversationId("conv_1");
    session.setStatus(BillStatus.FINALIZED);

    when(billSessionRepository.findById("bill_1")).thenReturn(Mono.just(session));
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(false));

    MarkBillSharePaidRequest request = new MarkBillSharePaidRequest();

    // Act & Assert
    StepVerifier.create(withUser(billService.markBillSharePaid("bill_1", request)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.NOT_A_PARTICIPANT)
        .verify();
  }

  @Test
  void getBillSession_WhenNotFound_ReturnsError() {
    // Arrange
    when(billSessionRepository.findById("bill_x")).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(withUser(billService.getBillSession("bill_x")))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.BILL_SESSION_NOT_FOUND)
        .verify();
  }

  @Test
  void getConversationBillSessions_ReturnsSessionsList() {
    // Arrange
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));

    BillSession session = new BillSession();
    session.setId("bill_1");
    when(billSessionRepository.findByConversationIdOrderByCreatedAtDesc("conv_1"))
        .thenReturn(Flux.just(session));

    BillSessionDTO dto =
        new BillSessionDTO(
            "bill_1",
            "conv_1",
            "vote_1",
            1L,
            "USD",
            new BigDecimal("10.00"),
            BillStatus.DRAFT,
            BillSplitType.EQUAL,
            List.of());
    when(billMapper.toDto(any(BillSession.class))).thenReturn(dto);

    // Act & Assert
    StepVerifier.create(withUser(billService.getConversationBillSessions("conv_1")))
        .assertNext(response -> assertEquals(1, response.getData().size()))
        .verifyComplete();
  }

  @Test
  void markBillSharePaid_UnauthorizedThirdParty_ReturnsError() {
    // Arrange
    BillSession session = new BillSession();
    session.setId("bill_1");
    session.setConversationId("conv_1");
    session.setCreatorId(99L);
    session.setStatus(BillStatus.FINALIZED);

    BillSession.BillShare share = new BillSession.BillShare();
    share.setUserId(3L);
    share.setAmount(new BigDecimal("5.00"));
    share.setStatus(BillShareStatus.UNPAID);
    session.setShares(new ArrayList<>(List.of(share)));

    MarkBillSharePaidRequest request = new MarkBillSharePaidRequest();
    request.setUserId(3L);

    when(billSessionRepository.findById("bill_1")).thenReturn(Mono.just(session));
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));

    // Act & Assert
    StepVerifier.create(withUser(billService.markBillSharePaid("bill_1", request)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.BILL_PAYMENT_UNAUTHORIZED)
        .verify();
  }
}
