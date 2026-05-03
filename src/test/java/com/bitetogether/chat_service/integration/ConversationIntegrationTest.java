package com.bitetogether.chat_service.integration;

import com.bitetogether.chat_service.enums.conversation.ConversationType;
import com.bitetogether.chat_service.enums.conversation.Role;
import com.bitetogether.chat_service.model.Conversation;
import com.bitetogether.chat_service.model.Participant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ConversationIntegrationTest extends BaseIntegrationTest {

  @Test
  void createGroupConversation_ReturnsCreated() {
    // Arrange & Act & Assert
    webTestClient
        .post()
        .uri("/api/v1/conversations")
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {
              "type": "GROUP",
              "name": "Integration Test Group",
              "participantIds": [2, 3]
            }
            """)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.data.id")
        .isNotEmpty()
        .jsonPath("$.data.name")
        .isEqualTo("Integration Test Group")
        .jsonPath("$.data.type")
        .isEqualTo("GROUP");
  }

  @Test
  void createGroupConversation_WithoutName_ReturnsBadRequest() {
    webTestClient
        .post()
        .uri("/api/v1/conversations")
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {
              "type": "GROUP",
              "name": "",
              "participantIds": [2, 3]
            }
            """)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void createDirectConversation_WithOneOtherUser_ReturnsCreated() {
    webTestClient
        .post()
        .uri("/api/v1/conversations")
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {
              "type": "DIRECT",
              "participantIds": [2]
            }
            """)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.data.type")
        .isEqualTo("DIRECT");
  }

  @Test
  void createDirectConversation_WithTooManyParticipants_ReturnsBadRequest() {
    webTestClient
        .post()
        .uri("/api/v1/conversations")
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {
              "type": "DIRECT",
              "participantIds": [2, 3]
            }
            """)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void getConversationById_WhenParticipant_ReturnsOk() {
    // Arrange - create conversation and participant directly in DB
    Conversation conversation = new Conversation();
    conversation.setType(ConversationType.GROUP);
    conversation.setName("Test Group");
    Conversation saved = conversationRepository.save(conversation).block();

    Participant participant = new Participant();
    participant.setConversationId(saved.getId());
    participant.setUserId(1L);
    participant.setRole(Role.ADMIN);
    participant.setLastReadMessageSequence(0L);
    participantRepository.save(participant).block();

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/v1/conversations/{id}", saved.getId())
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.id")
        .isEqualTo(saved.getId())
        .jsonPath("$.data.name")
        .isEqualTo("Test Group");
  }

  @Test
  void getConversationById_WhenNotParticipant_ReturnsForbidden() {
    // Arrange
    Conversation conversation = new Conversation();
    conversation.setType(ConversationType.GROUP);
    conversation.setName("Other Group");
    Conversation saved = conversationRepository.save(conversation).block();

    // No participant record for user 1

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/v1/conversations/{id}", saved.getId())
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void getMyConversations_ReturnsUserConversations() {
    // Arrange
    Conversation conversation = new Conversation();
    conversation.setType(ConversationType.GROUP);
    conversation.setName("My Group");
    Conversation saved = conversationRepository.save(conversation).block();

    Participant participant = new Participant();
    participant.setConversationId(saved.getId());
    participant.setUserId(1L);
    participant.setRole(Role.MEMBER);
    participant.setLastReadMessageSequence(0L);
    participantRepository.save(participant).block();

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/v1/conversations")
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.size")
        .isEqualTo(1)
        .jsonPath("$.data.conversations[0].name")
        .isEqualTo("My Group");
  }

  @Test
  void updateConversation_WhenAdmin_ReturnsOk() {
    // Arrange
    Conversation conversation = new Conversation();
    conversation.setType(ConversationType.GROUP);
    conversation.setName("Old Name");
    Conversation saved = conversationRepository.save(conversation).block();

    Participant participant = new Participant();
    participant.setConversationId(saved.getId());
    participant.setUserId(1L);
    participant.setRole(Role.ADMIN);
    participant.setLastReadMessageSequence(0L);
    participantRepository.save(participant).block();

    // Act & Assert
    webTestClient
        .put()
        .uri("/api/v1/conversations/{id}", saved.getId())
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"name": "New Name"}
            """)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.name")
        .isEqualTo("New Name");
  }

  @Test
  void updateConversation_WhenNotAdmin_ReturnsForbidden() {
    // Arrange
    Conversation conversation = new Conversation();
    conversation.setType(ConversationType.GROUP);
    conversation.setName("Group");
    Conversation saved = conversationRepository.save(conversation).block();

    Participant participant = new Participant();
    participant.setConversationId(saved.getId());
    participant.setUserId(1L);
    participant.setRole(Role.MEMBER);
    participant.setLastReadMessageSequence(0L);
    participantRepository.save(participant).block();

    // Act & Assert
    webTestClient
        .put()
        .uri("/api/v1/conversations/{id}", saved.getId())
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"name": "Hacked Name"}
            """)
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void deleteConversation_WhenAdmin_ReturnsOk() {
    // Arrange
    Conversation conversation = new Conversation();
    conversation.setType(ConversationType.GROUP);
    conversation.setName("To Delete");
    Conversation saved = conversationRepository.save(conversation).block();

    Participant participant = new Participant();
    participant.setConversationId(saved.getId());
    participant.setUserId(1L);
    participant.setRole(Role.ADMIN);
    participant.setLastReadMessageSequence(0L);
    participantRepository.save(participant).block();

    // Act & Assert
    webTestClient
        .delete()
        .uri("/api/v1/conversations/{id}", saved.getId())
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void addParticipant_WhenAdmin_ReturnsCreated() {
    // Arrange
    Conversation conversation = new Conversation();
    conversation.setType(ConversationType.GROUP);
    conversation.setName("Group");
    Conversation saved = conversationRepository.save(conversation).block();

    Participant admin = new Participant();
    admin.setConversationId(saved.getId());
    admin.setUserId(1L);
    admin.setRole(Role.ADMIN);
    admin.setLastReadMessageSequence(0L);
    participantRepository.save(admin).block();

    // Act & Assert
    webTestClient
        .post()
        .uri("/api/v1/conversations/{id}/participants", saved.getId())
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"userIds": [5, 6]}
            """)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.data.addedParticipants")
        .isArray()
        .jsonPath("$.data.skippedUserIds")
        .isArray();
  }

  @Test
  void removeParticipant_WhenAdmin_ReturnsOk() {
    // Arrange
    Conversation conversation = new Conversation();
    conversation.setType(ConversationType.GROUP);
    conversation.setName("Group");
    Conversation saved = conversationRepository.save(conversation).block();

    Participant admin = new Participant();
    admin.setConversationId(saved.getId());
    admin.setUserId(1L);
    admin.setRole(Role.ADMIN);
    admin.setLastReadMessageSequence(0L);
    participantRepository.save(admin).block();

    Participant member = new Participant();
    member.setConversationId(saved.getId());
    member.setUserId(5L);
    member.setRole(Role.MEMBER);
    member.setLastReadMessageSequence(0L);
    participantRepository.save(member).block();

    // Act & Assert
    webTestClient
        .delete()
        .uri("/api/v1/conversations/{id}/participants/{userId}", saved.getId(), 5L)
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  void withoutAuthHeader_ReturnsError() {
    webTestClient
        .get()
        .uri("/api/v1/conversations")
        .exchange()
        .expectStatus()
        .is5xxServerError(); // No user context -> error
  }
}
