package com.bitetogether.chat_service.integration;

import com.bitetogether.chat_service.repository.ConversationRepository;
import com.bitetogether.chat_service.repository.MessageRepository;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import com.bitetogether.chat_service.service.FirebaseStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

  @Autowired protected WebTestClient webTestClient;

  @Autowired protected ConversationRepository conversationRepository;

  @Autowired protected ParticipantRepository participantRepository;

  @Autowired protected MessageRepository messageRepository;

  // Mock Firebase since we can't run it in tests
  @MockitoBean protected FirebaseStorageService firebaseStorageService;

  // Mock the Google Cloud Storage bean required by FirebaseConfig
  @MockitoBean protected com.google.cloud.storage.Storage googleCloudStorage;

  @BeforeEach
  void cleanDatabase() {
    conversationRepository.deleteAll().block();
    participantRepository.deleteAll().block();
    messageRepository.deleteAll().block();
  }

  protected static final String USER_ID_HEADER = "X-User-Id";
  protected static final String USER_ROLE_HEADER = "X-User-Role";
  protected static final String USER_EMAIL_HEADER = "X-User-Email";
  protected static final String USERNAME_HEADER = "X-Username";
}
