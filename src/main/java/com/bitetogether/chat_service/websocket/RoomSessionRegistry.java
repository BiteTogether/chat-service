package com.bitetogether.chat_service.websocket;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;

@Component
public class RoomSessionRegistry {

  private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

  public void join(String roomId, WebSocketSession session) {
    rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
  }

  public void leave(WebSocketSession session) {
    rooms.values().forEach(set -> set.remove(session));
  }

  public void leave(String roomId, WebSocketSession session) {
    Set<WebSocketSession> roomSessions = rooms.get(roomId);
    if (roomSessions != null) {
      roomSessions.remove(session);
    }
  }

  public Flux<WebSocketSession> getSessions(String roomId) {
    return Flux.fromIterable(rooms.getOrDefault(roomId, Set.of()));
  }
}
