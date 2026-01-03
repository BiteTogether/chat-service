package com.bitetogether.chat_service.service;

import com.bitetogether.chat_service.dto.request.MessageRequest;
import com.bitetogether.chat_service.dto.response.MessageResponse;
import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.dto.ApiResponsePaginationDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageService {
  Mono<ApiResponseDTO<MessageResponse>> sendMessage(MessageRequest request);

  Mono<ApiResponseDTO<MessageResponse>> updateMessage(String messageId, MessageRequest request);

  Mono<ApiResponseDTO<Void>> deleteMessage(String messageId);

  Mono<ApiResponseDTO<MessageResponse>> getMessageById(String messageId);

  Mono<ApiResponsePaginationDTO<MessageResponse>> getMessagesByRoomPaginated(
      String roomId, int page, int size);

  Flux<ApiResponseDTO<MessageResponse>> streamMessages(String roomId);

  Flux<ApiResponseDTO<MessageResponse>> getMessageReplies(String messageId);

  Mono<MessageResponse> sendMessageDirect(MessageRequest request);

  Mono<MessageResponse> updateMessageDirect(String messageId, MessageRequest request);

  Mono<Void> deleteMessageDirect(String messageId);

  Mono<MessageResponse> getMessageByIdDirect(String messageId);

  Flux<MessageResponse> getMessagesByRoomDirect(String roomId);

  Flux<MessageResponse> streamMessagesDirect(String roomId);

  Flux<MessageResponse> getMessageRepliesDirect(String messageId);
}
