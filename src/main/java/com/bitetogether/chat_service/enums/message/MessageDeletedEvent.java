package com.bitetogether.chat_service.enums.message;

/** Event published when a message is deleted. */
public record MessageDeletedEvent(String conversationId, String messageId) {}
