package com.bitetogether.chat_service.dto.location;

import com.bitetogether.chat_service.dto.websocket.WsInboundEnvelope;
import com.bitetogether.chat_service.enums.websocket.WebSocketAction;

public record LocationUpdateInbound(
    String conversationId, WebSocketAction action, LocationPayload location, Boolean isSharing)
    implements WsInboundEnvelope {}
