package com.bitetogether.chat_service.enums;

public record EncryptedPayload(byte[] ciphertext, byte[] iv, byte[] authTag) {}
