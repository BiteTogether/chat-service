package com.bitetogether.chat_service.controller;

import com.bitetogether.chat_service.dto.request.MessageRequest;
import com.bitetogether.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@ActiveProfiles("test")
public class MessageControllerTest {

  @Autowired private RSocketRequester.Builder builder;

  @Test
  void testSendMessage() {
    RSocketRequester requester = builder.tcp("localhost", 7000);

    MessageRequest request = new MessageRequest();
    request.setRoomId("room1");
    request.setSenderId("user1");
    request.setContent("hello");
    request.setType(com.bitetogether.chat_service.enums.MessageType.TEXT);

    Mono<ApiResponse> response =
        requester.route("message.send").data(request).retrieveMono(ApiResponse.class);

    StepVerifier.create(response).expectNextMatches(r -> r.getStatus() == 200).verifyComplete();
  }
}
