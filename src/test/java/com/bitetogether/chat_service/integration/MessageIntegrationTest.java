package com.bitetogether.chat_service.integration;

import com.bitetogether.chat_service.enums.conversation.ConversationType;
import com.bitetogether.chat_service.enums.conversation.Role;
import com.bitetogether.chat_service.model.Conversation;
import com.bitetogether.chat_service.model.Participant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class MessageIntegrationTest extends BaseIntegrationTest {

  private String setupConversationWithParticipant() {
    Conversation conversation = new Conversation();
    conversation.setType(ConversationType.GROUP);
    conversation.setName("Test");
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
  void sendMessage_WhenParticipant_ReturnsCreated() {
    // Arrange
    String conversationId = setupConversationWithParticipant();

    // Act & Assert
    webTestClient
        .post()
        .uri("/api/v1/messages")
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
                  "messageType": "TEXT",
                  "content": "Hello from integration test!"
                }
                """,
                conversationId))
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.data.id")
        .isNotEmpty()
        .jsonPath("$.data.content")
        .isEqualTo("Hello from integration test!")
        .jsonPath("$.data.conversationId")
        .isEqualTo(conversationId)
        .jsonPath("$.data.senderId")
        .isEqualTo(1)
        .jsonPath("$.data.seq")
        .isEqualTo(1);
  }

  @Test
  void sendMessage_WhenNotParticipant_ReturnsForbidden() {
    // Arrange - conversation exists but user 99 is not a participant
    Conversation conversation = new Conversation();
    conversation.setType(ConversationType.GROUP);
    conversation.setName("Other");
    Conversation saved = conversationRepository.save(conversation).block();

    // Act & Assert
    webTestClient
        .post()
        .uri("/api/v1/messages")
        .header(USER_ID_HEADER, "99")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user99@test.com")
        .header(USERNAME_HEADER, "user99")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            String.format(
                """
                {
                  "conversationId": "%s",
                  "messageType": "TEXT",
                  "content": "Should fail"
                }
                """,
                saved.getId()))
        .exchange()
        .expectStatus()
        .isForbidden();
  }

  @Test
  void getMessages_WhenParticipant_ReturnsMessages() {
    // Arrange - send a message first
    String conversationId = setupConversationWithParticipant();

    webTestClient
        .post()
        .uri("/api/v1/messages")
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            String.format(
                """
                {"conversationId": "%s", "messageType": "TEXT", "content": "msg1"}
                """,
                conversationId))
        .exchange()
        .expectStatus()
        .isCreated();

    // Act & Assert
    webTestClient
        .get()
        .uri("/api/v1/messages/conversation/{id}", conversationId)
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.messages")
        .isArray()
        .jsonPath("$.data.messages[0].content")
        .isEqualTo("msg1")
        .jsonPath("$.data.size")
        .isEqualTo(1)
        .jsonPath("$.data.hasMore")
        .isEqualTo(false);
  }

  @Test
  void sendMultipleMessages_SequenceIncrementsCorrectly() {
    // Arrange
    String conversationId = setupConversationWithParticipant();

    // Act - send 3 messages
    for (int i = 1; i <= 3; i++) {
      webTestClient
          .post()
          .uri("/api/v1/messages")
          .header(USER_ID_HEADER, "1")
          .header(USER_ROLE_HEADER, "USER")
          .header(USER_EMAIL_HEADER, "user1@test.com")
          .header(USERNAME_HEADER, "user1")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
              String.format(
                  """
                  {"conversationId": "%s", "messageType": "TEXT", "content": "msg%d"}
                  """,
                  conversationId, i))
          .exchange()
          .expectStatus()
          .isCreated()
          .expectBody()
          .jsonPath("$.data.seq")
          .isEqualTo(i);
    }

    // Assert - get messages and verify ordering
    webTestClient
        .get()
        .uri("/api/v1/messages/conversation/{id}", conversationId)
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.size")
        .isEqualTo(3)
        .jsonPath("$.data.messages[0].seq")
        .isEqualTo(3) // newest first
        .jsonPath("$.data.messages[2].seq")
        .isEqualTo(1);
  }

  @Test
  void updateMessage_WhenOwner_ReturnsOk() {
    // Arrange
    String conversationId = setupConversationWithParticipant();

    // Send a message first
    webTestClient
        .post()
        .uri("/api/v1/messages")
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            String.format(
                """
                {"conversationId": "%s", "messageType": "TEXT", "content": "original"}
                """,
                conversationId))
        .exchange()
        .expectStatus()
        .isCreated();

    String messageId = extractMessageId(conversationId);

    // Act & Assert
    webTestClient
        .put()
        .uri("/api/v1/messages/{id}", messageId)
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            """
            {"content": "updated content"}
            """)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .jsonPath("$.data.content")
        .isEqualTo("updated content");
  }

  @Test
  void deleteMessage_WhenOwner_ReturnsOk() {
    // Arrange
    String conversationId = setupConversationWithParticipant();

    // Send a message
    webTestClient
        .post()
        .uri("/api/v1/messages")
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            String.format(
                """
                {"conversationId": "%s", "messageType": "TEXT", "content": "to delete"}
                """,
                conversationId))
        .exchange()
        .expectStatus()
        .isCreated();

    String messageId = extractMessageId(conversationId);

    // Act & Assert
    webTestClient
        .delete()
        .uri("/api/v1/messages/{id}", messageId)
        .header(USER_ID_HEADER, "1")
        .header(USER_ROLE_HEADER, "USER")
        .header(USER_EMAIL_HEADER, "user1@test.com")
        .header(USERNAME_HEADER, "user1")
        .exchange()
        .expectStatus()
        .isOk();
  }

  private String extractMessageId(String conversationId) {
    // Fetch messages to get the ID
    byte[] body =
        webTestClient
            .get()
            .uri("/api/v1/messages/conversation/{id}", conversationId)
            .header(USER_ID_HEADER, "1")
            .header(USER_ROLE_HEADER, "USER")
            .header(USER_EMAIL_HEADER, "user1@test.com")
            .header(USERNAME_HEADER, "user1")
            .exchange()
            .expectBody()
            .returnResult()
            .getResponseBody();

    // Parse JSON to get the first message ID
    try {
      com.fasterxml.jackson.databind.JsonNode node =
          new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
      return node.path("data").path("messages").get(0).path("id").asText();
    } catch (Exception e) {
      throw new RuntimeException("Failed to extract message ID", e);
    }
  }
}
