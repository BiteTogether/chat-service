package com.bitetogether.chat_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bitetogether.chat_service.enums.crypto.EncryptedPayload;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class CryptoServiceTest {

  private CryptoService cryptoService;

  @BeforeEach
  void setUp() throws Exception {
    KeyGenerator keyGen = KeyGenerator.getInstance("AES");
    keyGen.init(256);
    SecretKey secretKey = keyGen.generateKey();
    cryptoService = new CryptoService(secretKey);
  }

  @Test
  void encrypt_ShouldReturnNonNullPayload() {
    // Arrange
    String plaintext = "Hello, World!";

    // Act & Assert
    StepVerifier.create(cryptoService.encrypt(plaintext))
        .assertNext(
            payload -> {
              assertNotNull(payload.ciphertext());
              assertNotNull(payload.iv());
              assertNotNull(payload.authTag());
              assertEquals(12, payload.iv().length);
              assertEquals(16, payload.authTag().length);
            })
        .verifyComplete();
  }

  @Test
  void decrypt_ShouldRecoverOriginalPlaintext() {
    // Arrange
    String plaintext = "Hello, World!";

    // Act & Assert
    StepVerifier.create(
            cryptoService
                .encrypt(plaintext)
                .flatMap(
                    payload ->
                        cryptoService.decrypt(
                            payload.ciphertext(), payload.iv(), payload.authTag())))
        .expectNext(plaintext)
        .verifyComplete();
  }

  @Test
  void encrypt_ShouldProduceDifferentIVsEachTime() {
    // Arrange
    String plaintext = "same text";

    // Act & Assert
    StepVerifier.create(cryptoService.encrypt(plaintext).zipWith(cryptoService.encrypt(plaintext)))
        .assertNext(
            tuple -> {
              EncryptedPayload first = tuple.getT1();
              EncryptedPayload second = tuple.getT2();
              assertFalse(java.util.Arrays.equals(first.iv(), second.iv()));
            })
        .verifyComplete();
  }

  @Test
  void decrypt_WithTamperedCiphertext_ShouldFail() {
    // Arrange
    String plaintext = "secret";

    // Act & Assert
    StepVerifier.create(
            cryptoService
                .encrypt(plaintext)
                .flatMap(
                    payload -> {
                      byte[] tampered = payload.ciphertext().clone();
                      tampered[0] ^= 0xFF;
                      return cryptoService.decrypt(tampered, payload.iv(), payload.authTag());
                    }))
        .expectError()
        .verify();
  }

  @Test
  void encrypt_WithEmptyString_ShouldSucceed() {
    // Arrange
    String plaintext = "";

    // Act & Assert
    StepVerifier.create(
            cryptoService
                .encrypt(plaintext)
                .flatMap(p -> cryptoService.decrypt(p.ciphertext(), p.iv(), p.authTag())))
        .expectNext("")
        .verifyComplete();
  }

  @Test
  void encrypt_WithUnicodeCharacters_ShouldRoundTrip() {
    // Arrange
    String plaintext = "こんにちは世界 🌍";

    // Act & Assert
    StepVerifier.create(
            cryptoService
                .encrypt(plaintext)
                .flatMap(p -> cryptoService.decrypt(p.ciphertext(), p.iv(), p.authTag())))
        .expectNext(plaintext)
        .verifyComplete();
  }
}
