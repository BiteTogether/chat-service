package com.bitetogether.chat_service.configuration.firebase;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "firebase")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FirebaseProperties {
  String credentialsPath;
  String storageBucket;
  String baseUrl;
}
