package com.bitetogether.chat_service.configuration.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FirebaseConfig {

  private final FirebaseProperties firebaseProperties;

  private InputStream getCredentialsInputStream() throws IOException {
    String credentialsPath = firebaseProperties.getCredentialsPath();

    // Try file system first (for Docker)
    if (Files.exists(Paths.get(credentialsPath))) {
      log.info("Loading Firebase credentials from file system: {}", credentialsPath);
      return new FileInputStream(credentialsPath);
    }

    // Fallback to classpath (for local development)
    log.info("Loading Firebase credentials from classpath: {}", credentialsPath);
    return new ClassPathResource(credentialsPath).getInputStream();
  }

  @Bean
  public FirebaseApp initializeFirebase() throws IOException {
    if (FirebaseApp.getApps().isEmpty()) {
      try (InputStream serviceAccount = getCredentialsInputStream()) {
        FirebaseOptions options =
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setStorageBucket(firebaseProperties.getStorageBucket())
                .build();

        FirebaseApp app = FirebaseApp.initializeApp(options);
        log.info("Firebase application has been initialized successfully");
        return app;
      }
    }
    return FirebaseApp.getInstance();
  }

  @Bean
  public Storage storage() throws IOException {
    try (InputStream serviceAccount = getCredentialsInputStream()) {
      GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
      return StorageOptions.newBuilder().setCredentials(credentials).build().getService();
    }
  }
}
