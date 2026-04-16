package com.bitetogether.chat_service.service;

import com.bitetogether.chat_service.enums.crypto.EncryptedPayload;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class CryptoService {

  private static final String ALGO = "AES/GCM/NoPadding";
  private static final int IV_LENGTH = 12;
  private static final int TAG_LENGTH_BIT = 128;

  SecretKey secretKey;
  SecureRandom secureRandom = new SecureRandom();

  public Mono<EncryptedPayload> encrypt(String plaintext) {
    return Mono.fromCallable(
        () -> {
          byte[] iv = new byte[IV_LENGTH];
          secureRandom.nextBytes(iv);

          Cipher cipher = Cipher.getInstance(ALGO);
          GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
          cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

          byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

          int tagLength = 16;
          byte[] ciphertext = Arrays.copyOfRange(encrypted, 0, encrypted.length - tagLength);
          byte[] authTag =
              Arrays.copyOfRange(encrypted, encrypted.length - tagLength, encrypted.length);

          return new EncryptedPayload(ciphertext, iv, authTag);
        });
  }

  public Mono<String> decrypt(byte[] ciphertext, byte[] iv, byte[] authTag) {
    return Mono.fromCallable(
        () -> {
          Cipher cipher = Cipher.getInstance(ALGO);
          GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
          cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

          byte[] combined =
              ByteBuffer.allocate(ciphertext.length + authTag.length)
                  .put(ciphertext)
                  .put(authTag)
                  .array();

          byte[] decrypted = cipher.doFinal(combined);

          return new String(decrypted, StandardCharsets.UTF_8);
        });
  }
}
