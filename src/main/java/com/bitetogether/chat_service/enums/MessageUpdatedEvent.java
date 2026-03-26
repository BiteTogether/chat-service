package com.bitetogether.chat_service.enums;

import com.bitetogether.chat_service.dto.message.ChatMessageDTO;

/** Event published when a message is updated. */
public record MessageUpdatedEvent(String conversationId, ChatMessageDTO message) {}
