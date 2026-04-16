package com.bitetogether.chat_service.dto.websocket;

import com.bitetogether.chat_service.enums.websocket.WebSocketAction;

public record WsEnvelopeDTO(String conversationId, WebSocketAction action)
    implements WsInboundEnvelope {}
