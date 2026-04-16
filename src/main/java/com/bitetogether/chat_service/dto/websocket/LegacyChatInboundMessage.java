package com.bitetogether.chat_service.dto.websocket;

import com.bitetogether.chat_service.dto.location.LocationPayload;
import com.bitetogether.chat_service.enums.message.MessageType;
import com.bitetogether.chat_service.enums.websocket.WebSocketAction;

public record LegacyChatInboundMessage(
    String conversationId,
    WebSocketAction action,
    MessageType messageType,
    String content,
    LocationPayload location,
    Boolean isSharing) {}
