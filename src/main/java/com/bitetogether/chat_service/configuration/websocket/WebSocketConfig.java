package com.bitetogether.chat_service.configuration.websocket;

import com.bitetogether.chat_service.websocket.ChatWebSocketHandler;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

@Component
public class WebSocketConfig {
  @Bean
  public HandlerMapping webSocketMapping(ChatWebSocketHandler handler) {
    Map<String, WebSocketHandler> map = Map.of("/ws/chat", handler);
    SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
    mapping.setUrlMap(map);
    mapping.setOrder(-1);
    return mapping;
  }

  @Bean
  public WebSocketHandlerAdapter handlerAdapter() {
    return new WebSocketHandlerAdapter();
  }
}
