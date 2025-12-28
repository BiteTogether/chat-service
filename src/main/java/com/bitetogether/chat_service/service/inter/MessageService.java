package com.bitetogether.chat_service.service.inter;

import com.bitetogether.chat_service.dto.request.MessageRequest;
import com.bitetogether.chat_service.dto.response.MessageResponse;
import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.dto.ApiResponsePagination;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageService {
  // REST API methods (return ApiResponse wrapper)
  Mono<ApiResponse<MessageResponse>> sendMessage(MessageRequest request, String authorization);

  Mono<ApiResponse<MessageResponse>> updateMessage(String messageId, MessageRequest request, String authorization);

  Mono<ApiResponse<Void>> deleteMessage(String messageId);

  Mono<ApiResponse<MessageResponse>> getMessageById(String messageId, String authorization);

  Mono<ApiResponsePagination<MessageResponse>> getMessagesByRoomPaginated(
      String roomId, int page, int size, String authorization);

  Flux<ApiResponse<MessageResponse>> streamMessages(String roomId, String authorization);

  Flux<ApiResponse<MessageResponse>> getMessageReplies(String messageId, String authorization);

  // Direct methods for RSocket (return raw data without wrapper)
  Mono<MessageResponse> sendMessageDirect(MessageRequest request, String authorization);

  Mono<MessageResponse> updateMessageDirect(String messageId, MessageRequest request, String authorization);

  Mono<Void> deleteMessageDirect(String messageId);

  Mono<MessageResponse> getMessageByIdDirect(String messageId, String authorization);

  Flux<MessageResponse> getMessagesByRoomDirect(String roomId, String authorization);

  Flux<MessageResponse> streamMessagesDirect(String roomId, String authorization);

  Flux<MessageResponse> getMessageRepliesDirect(String messageId, String authorization);
}
