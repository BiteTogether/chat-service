package com.bitetogether.chat_service.integration;

import com.bitetogether.chat_service.enums.conversation.ConversationType;
import com.bitetogether.chat_service.enums.conversation.Role;
import com.bitetogether.chat_service.enums.vote.VoteSessionStatus;
import com.bitetogether.chat_service.model.Conversation;
import com.bitetogether.chat_service.model.Participant;
import com.bitetogether.chat_service.model.VoteSession;
import com.bitetogether.chat_service.repository.VoteSessionRepository;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class VoteIntegrationTest extends BaseIntegrationTest {

  @Autowired private VoteSessionRepository voteSessionRepository;

  private String setupConversationWithParticipant() {
    Conversation conversation = new Conversation();
    conversation.setType(ConversationType.GROUP);
    conversation.setName("Vote Group");
    Conversation saved = conversationRepository.save(conversation).block();

    Participant participant = new Participant();
    participant.setConversationId(saved.getId());
    participant.setUserId(1L);
    participant.setRole(Role.MEMBER);
    participant.setLastReadMessageSequence(0L);
    participantRepository.save(participant).block();

    return saved.getId();
  }

  @Test
  void createVoteSession_WhenParticipant_ReturnsCreated() {
    // Arrange
    String conversationId = setupConversationWithParticipant();

    // Act & Assert
    webTestClient
        .post()
        .uri("/api/v1/votes")
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            String.format(
                """
                {
                  "conversationId": "%s",
                  "name": "Where to eat?",
                  "options": [
                    {"placeId": "place_1", "name": "Restaurant A"},
                    {"placeId": "place_2", "name": "Restaurant B"}
                  ]
                }
                """,
                conversationId))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.data.id")
        .isNotEmpty()
        .jsonPath("$.data.name")
        .isEqualTo("Where to eat?")
        .jsonPath("$.data.status")
        .isEqualTo("OPEN");
  }

  @Test
  void castVote_WhenOpen_ReturnsOk() {
    // Arrange
    String conversationId = setupConversationWithParticipant();

    VoteSession session = new VoteSession();
    session.setConversationId(conversationId);
    session.setCreatorId(1L);
    session.setName("Test Vote");
    session.setStatus(VoteSessionStatus.OPEN);

    VoteSession.VoteOption option = new VoteSession.VoteOption();
    option.setId("opt_1");
    option.setPlaceId("place_1");
    option.setName("Place A");
    option.setLabel("Place A");
    session.setOptions(List.of(option));
    session.setVotes(new HashMap<>());

    VoteSession saved = voteSessionRepository.save(session).block();

    // Act & Assert
    webTestClient
        .post()
        .uri("/api/v1/votes/{id}/cast", saved.getId())
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"optionId": "opt_1"}
            """)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.votes.1")
        .isEqualTo("opt_1");
  }

  @Test
  void closeVoteSession_WhenOpen_ReturnsOk() {
    // Arrange
    String conversationId = setupConversationWithParticipant();

    VoteSession session = new VoteSession();
    session.setConversationId(conversationId);
    session.setCreatorId(1L);
    session.setName("To Close");
    session.setStatus(VoteSessionStatus.OPEN);
    session.setOptions(List.of());
    session.setVotes(new HashMap<>());

    VoteSession saved = voteSessionRepository.save(session).block();

    // Act & Assert
    webTestClient
        .post()
        .uri("/api/v1/votes/{id}/close", saved.getId())
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.status")
        .isEqualTo("CLOSED");
  }

  @Test
  void getVoteSession_WhenParticipant_ReturnsOk() {
    // Arrange
    String conversationId = setupConversationWithParticipant();

    VoteSession session = new VoteSession();
    session.setConversationId(conversationId);
    session.setCreatorId(1L);
    session.setName("Visible Vote");
    session.setStatus(VoteSessionStatus.OPEN);
    session.setOptions(List.of());
    session.setVotes(new HashMap<>());

    VoteSession saved = voteSessionRepository.save(session).block();

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/v1/votes/{id}", saved.getId())
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.name")
        .isEqualTo("Visible Vote");
  }

  @Test
  void getConversationVoteSessions_ReturnsAll() {
    // Arrange
    String conversationId = setupConversationWithParticipant();

    VoteSession s1 = new VoteSession();
    s1.setConversationId(conversationId);
    s1.setCreatorId(1L);
    s1.setName("Vote 1");
    s1.setStatus(VoteSessionStatus.OPEN);
    s1.setOptions(List.of());
    s1.setVotes(new HashMap<>());
    voteSessionRepository.save(s1).block();

    VoteSession s2 = new VoteSession();
    s2.setConversationId(conversationId);
    s2.setCreatorId(1L);
    s2.setName("Vote 2");
    s2.setStatus(VoteSessionStatus.CLOSED);
    s2.setOptions(List.of());
    s2.setVotes(new HashMap<>());
    voteSessionRepository.save(s2).block();

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/v1/votes/conversation/{id}", conversationId)
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.length()")
        .isEqualTo(2);
  }
}
