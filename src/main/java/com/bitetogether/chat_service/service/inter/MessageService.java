package com.bitetogether.chat_service.service.inter;

import com.bitetogether.chat_service.dto.request.MessageRequest;
import com.bitetogether.chat_service.dto.response.MessageResponse;
import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.dto.ApiResponsePagination;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageService {
  // REST API methods (return ApiResponse wrapper)
  Mono<ApiResponse<MessageResponse>> sendMessage(MessageRequest request);

  Mono<ApiResponse<MessageResponse>> updateMessage(String messageId, MessageRequest request);

  Mono<ApiResponse<Void>> deleteMessage(String messageId);

  Mono<ApiResponse<MessageResponse>> getMessageById(String messageId);

  Mono<ApiResponsePagination<MessageResponse>> getMessagesByRoomPaginated(
      String roomId, int page, int size);

  Flux<ApiResponse<MessageResponse>> streamMessages(String roomId);

  Flux<ApiResponse<MessageResponse>> getMessageReplies(String messageId);

  // Direct methods for RSocket (return raw data without wrapper)
  Mono<MessageResponse> sendMessageDirect(MessageRequest request);

  Mono<MessageResponse> updateMessageDirect(String messageId, MessageRequest request);

  Mono<Void> deleteMessageDirect(String messageId);

  Mono<MessageResponse> getMessageByIdDirect(String messageId);

  Flux<MessageResponse> getMessagesByRoomDirect(String roomId);

  Flux<MessageResponse> streamMessagesDirect(String roomId);

  Flux<MessageResponse> getMessageRepliesDirect(String messageId);
}
