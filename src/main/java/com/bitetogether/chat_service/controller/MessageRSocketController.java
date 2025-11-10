package com.bitetogether.chat_service.controller;

import com.bitetogether.chat_service.dto.request.MessageRequest;
import com.bitetogether.chat_service.dto.response.MessageResponse;
import com.bitetogether.chat_service.service.inter.MessageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageRSocketController {

  MessageService messageService;

  @MessageMapping("message.send")
  public Mono<MessageResponse> sendMessage(@Payload MessageRequest request) {
    return messageService.sendMessageDirect(request);
  }

  @MessageMapping("message.update")
  public Mono<MessageResponse> updateMessage(@Payload UpdateMessageRequest request) {
    return messageService.updateMessageDirect(request.getMessageId(), request);
  }

  @MessageMapping("message.delete")
  public Mono<Void> deleteMessage(@Payload String messageId) {
    return messageService.deleteMessageDirect(messageId);
  }

  @MessageMapping("message.getById")
  public Mono<MessageResponse> getMessageById(@Payload String messageId) {
    return messageService.getMessageByIdDirect(messageId);
  }

  @MessageMapping("message.getByRoom")
  public Flux<MessageResponse> getMessagesByRoom(@Payload String roomId) {
    return messageService.getMessagesByRoomDirect(roomId);
  }

  @MessageMapping("message.stream")
  public Flux<MessageResponse> streamMessages(@Payload String roomId) {
    return messageService.streamMessagesDirect(roomId);
  }

  // Inner class for update request with messageId
  public static class UpdateMessageRequest extends MessageRequest {
    private String messageId;

    public String getMessageId() {
      return messageId;
    }

    public void setMessageId(String messageId) {
      this.messageId = messageId;
    }
  }
}
