package com.bitetogether.chat_service.enums;

import com.bitetogether.chat_service.dto.message.ChatMessageDTO;

public record MessageCreatedEvent(String conversationId, ChatMessageDTO message) {}
