package com.bitetogether.chat_service.configuration.crypto;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {

  @Value("${chat.crypto.key}")
  private String base64Key;

  @Bean
  public SecretKey aesKey() {
    byte[] decoded = Base64.getDecoder().decode(base64Key);
    return new SecretKeySpec(decoded, "AES");
  }
}
