package com.bitetogether.chat_service.enums.crypto;

public record EncryptedPayload(byte[] ciphertext, byte[] iv, byte[] authTag) {}
