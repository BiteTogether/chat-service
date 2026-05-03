package com.bitetogether.chat_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.bitetogether.chat_service.event.DomainEventPublisher;
import com.bitetogether.chat_service.exception.ErrorCode;
import com.bitetogether.chat_service.mapper.VoteMapper;
import com.bitetogether.chat_service.model.VoteSession;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import com.bitetogether.chat_service.repository.VoteSessionRepository;
import com.bitetogether.common.exception.AppException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

  @Mock private VoteSessionRepository voteSessionRepository;
  @Mock private ParticipantRepository participantRepository;
  @Mock private DomainEventPublisher domainEventPublisher;
  @Mock private VoteMapper voteMapper;

  private VoteService voteService;

  @BeforeEach
  void setUp() {
    voteService =
        new VoteService(
            voteSessionRepository, participantRepository, domainEventPublisher, voteMapper);
  }

  @Test
  void resolveVoteParticipantIds_WithValidSession_ReturnsVoterIds() {
    // Arrange
    VoteSession session = new VoteSession();
    session.setConversationId("conv_1");
    Map<Long, String> votes = new HashMap<>();
    votes.put(1L, "opt_1");
    votes.put(2L, "opt_2");
    session.setVotes(votes);

    when(voteSessionRepository.findById("vote_1")).thenReturn(Mono.just(session));

    // Act & Assert
    StepVerifier.create(voteService.resolveVoteParticipantIds("conv_1", "vote_1"))
        .assertNext(
            ids -> {
              assertEquals(2, ids.size());
              assertTrue(ids.contains(1L));
              assertTrue(ids.contains(2L));
            })
        .verifyComplete();
  }

  @Test
  void resolveVoteParticipantIds_WhenSessionNotFound_ReturnsError() {
    // Arrange
    when(voteSessionRepository.findById("vote_x")).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(voteService.resolveVoteParticipantIds("conv_1", "vote_x"))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.VOTE_SESSION_NOT_FOUND)
        .verify();
  }

  @Test
  void resolveVoteParticipantIds_WhenConversationMismatch_ReturnsError() {
    // Arrange
    VoteSession session = new VoteSession();
    session.setConversationId("conv_other");
    when(voteSessionRepository.findById("vote_1")).thenReturn(Mono.just(session));

    // Act & Assert
    StepVerifier.create(voteService.resolveVoteParticipantIds("conv_1", "vote_1"))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.VOTE_SESSION_NOT_FOUND)
        .verify();
  }

  @Test
  void resolveVoteParticipantIds_WithNullVoteSessionId_ReturnsError() {
    // Act & Assert
    StepVerifier.create(voteService.resolveVoteParticipantIds("conv_1", null))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.VOTE_SESSION_NOT_FOUND)
        .verify();
  }

  @Test
  void resolveVoteParticipantIds_WithNullVotes_ReturnsEmptyList() {
    // Arrange
    VoteSession session = new VoteSession();
    session.setConversationId("conv_1");
    session.setVotes(null);
    when(voteSessionRepository.findById("vote_1")).thenReturn(Mono.just(session));

    // Act & Assert
    StepVerifier.create(voteService.resolveVoteParticipantIds("conv_1", "vote_1"))
        .assertNext(ids -> assertTrue(ids.isEmpty()))
        .verifyComplete();
  }

  @Test
  void validateVoteSessionCreator_WhenCreator_Completes() {
    // Arrange
    VoteSession session = new VoteSession();
    session.setConversationId("conv_1");
    session.setCreatorId(1L);
    when(voteSessionRepository.findById("vote_1")).thenReturn(Mono.just(session));

    // Act & Assert
    StepVerifier.create(voteService.validateVoteSessionCreator("conv_1", "vote_1", 1L))
        .verifyComplete();
  }

  @Test
  void validateVoteSessionCreator_WhenNotCreator_ReturnsError() {
    // Arrange
    VoteSession session = new VoteSession();
    session.setConversationId("conv_1");
    session.setCreatorId(1L);
    when(voteSessionRepository.findById("vote_1")).thenReturn(Mono.just(session));

    // Act & Assert
    StepVerifier.create(voteService.validateVoteSessionCreator("conv_1", "vote_1", 99L))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode()
                        == ErrorCode.CONVERSATION_UPDATE_UNAUTHORIZED)
        .verify();
  }
}
