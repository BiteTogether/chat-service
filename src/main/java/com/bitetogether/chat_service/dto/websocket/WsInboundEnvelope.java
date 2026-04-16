package com.bitetogether.chat_service.dto.websocket;

import com.bitetogether.chat_service.enums.websocket.WebSocketAction;

public interface WsInboundEnvelope {
  String conversationId();

  WebSocketAction action();
}
