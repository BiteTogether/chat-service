package com.bitetogether.chat_service.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler implements WebSocketHandler {

    private final RoomSessionRegistry registry;
    private final ObjectMapper mapper;
    private final MessageService messageService;

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> {
                    ChatInboundMessage msg = mapper.readValue(payload, ChatInboundMessage.class);

                    if ("SUBSCRIBE".equals(msg.getType())) {
                        registry.join(msg.getConversationId(), session);
                        return Mono.empty();
                    }

                    if ("SEND".equals(msg.getType())) {
                        return messageService.processIncoming(msg)
                                .flatMap(saved ->
                                        broadcast(msg.getConversationId(), saved));
                    }

                    return Mono.empty();
                })
                .doFinally(s -> registry.leave(session))
                .then();
    }

    private Mono<Void> broadcast(String roomId, ChatMessageDto dto) {
        return registry.getSessions(roomId)
                .flatMap(s ->
                        s.send(Mono.just(
                                s.textMessage(mapper.writeValueAsString(dto))
                        ))
                ).then();
    }
}

