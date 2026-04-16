package com.bitetogether.chat_service.dto.message;

import com.bitetogether.chat_service.dto.websocket.WsInboundEnvelope;
import com.bitetogether.chat_service.enums.message.MessageType;
import com.bitetogether.chat_service.enums.websocket.WebSocketAction;

public record SendMessageInbound(
    String conversationId, WebSocketAction action, MessageType messageType, String content)
    implements WsInboundEnvelope {}
