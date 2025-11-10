package com.bitetogether.chat_service.service.impl;

import com.bitetogether.chat_service.dto.request.MessageRequest;
import com.bitetogether.chat_service.dto.response.MessageResponse;
import com.bitetogether.chat_service.mapper.MessageMapper;
import com.bitetogether.chat_service.model.Message;
import com.bitetogether.chat_service.repository.MessageRepository;
import com.bitetogether.chat_service.service.inter.MessageService;
import com.bitetogether.common.dto.ApiResponse;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.util.ApiResponseUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageServiceImpl implements MessageService {

  MessageRepository messageRepository;
  MessageMapper messageMapper;

  // Sink for real-time message streaming
  Sinks.Many<MessageResponse> messageSink = Sinks.many().multicast().onBackpressureBuffer();

  // =========================================================
  // =============== REST API METHODS ========================
  // =========================================================

  @Override
  public Mono<ApiResponse<MessageResponse>> sendMessage(MessageRequest request) {
    log.info("REST: Sending message to room: {}", request.getRoomId());
    return sendMessageDirect(request)
        .map(
            response ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.SUCCESS, "Message sent successfully", response))
        .onErrorResume(
            e -> {
              log.error("Failed to send message: {}", e.getMessage());
              return Mono.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST,
                      "Failed to send message: " + e.getMessage(),
                      null));
            });
  }

  @Override
  public Mono<ApiResponse<MessageResponse>> updateMessage(
      String messageId, MessageRequest request) {
    log.info("REST: Updating message: {}", messageId);
    return updateMessageDirect(messageId, request)
        .map(
            response ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.SUCCESS, "Message updated successfully", response))
        .onErrorResume(
            e -> {
              log.error("Failed to update message: {}", e.getMessage());
              return Mono.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST,
                      "Failed to update message: " + e.getMessage(),
                      null));
            });
  }

  @Override
  public Mono<ApiResponse<Void>> deleteMessage(String messageId) {
    log.info("REST: Deleting message: {}", messageId);
    return deleteMessageDirect(messageId)
        .then(
            Mono.fromCallable(
                () ->
                    ApiResponseUtil.<Void>buildApiResponse(
                        ApiResponseStatus.SUCCESS, "Message deleted successfully", null)))
        .onErrorResume(
            e -> {
              log.error("Failed to delete message: {}", e.getMessage());
              return Mono.just(
                  ApiResponseUtil.<Void>buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST,
                      "Failed to delete message: " + e.getMessage(),
                      null));
            });
  }

  @Override
  public Mono<ApiResponse<MessageResponse>> getMessageById(String messageId) {
    log.info("REST: Getting message by id: {}", messageId);
    return getMessageByIdDirect(messageId)
        .map(
            response ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.SUCCESS, "Message retrieved successfully", response))
        .onErrorResume(
            e -> {
              log.error("Failed to get message: {}", e.getMessage());
              return Mono.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST, "Message not found: " + e.getMessage(), null));
            });
  }

  @Override
  public Flux<ApiResponse<MessageResponse>> getMessagesByRoom(String roomId) {
    log.info("REST: Getting messages for room: {}", roomId);
    return getMessagesByRoomDirect(roomId)
        .map(
            response ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.SUCCESS, "Messages retrieved successfully", response))
        .onErrorResume(
            e -> {
              log.error("Failed to get messages: {}", e.getMessage());
              return Flux.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST,
                      "Failed to fetch messages: " + e.getMessage(),
                      null));
            });
  }

  @Override
  public Flux<ApiResponse<MessageResponse>> streamMessages(String roomId) {
    log.info("REST: Streaming messages for room: {}", roomId);
    return streamMessagesDirect(roomId)
        .map(
            response ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.SUCCESS, "Message streamed", response))
        .onErrorResume(
            e -> {
              log.error("Failed to stream messages: {}", e.getMessage());
              return Flux.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST,
                      "Failed to stream messages: " + e.getMessage(),
                      null));
            });
  }

  // =========================================================
  // =============== DIRECT METHODS (for RSocket) ===========
  // =========================================================

  @Override
  public Mono<MessageResponse> sendMessageDirect(MessageRequest request) {
    log.info("Direct: Sending message to room: {}", request.getRoomId());
    Message message = messageMapper.toMessage(request);

    return messageRepository
        .save(message)
        .map(messageMapper::toMessageResponse)
        .doOnNext(
            response -> {
              log.info("Message saved with id: {}", response.getId());
              // Emit to sink for real-time streaming
              messageSink.tryEmitNext(response);
            })
        .doOnError(e -> log.error("Error saving message: {}", e.getMessage()));
  }

  @Override
  public Mono<MessageResponse> updateMessageDirect(String messageId, MessageRequest request) {
    log.info("Direct: Updating message: {}", messageId);

    return messageRepository
        .findById(messageId)
        .switchIfEmpty(Mono.error(new RuntimeException("Message not found with id: " + messageId)))
        .flatMap(
            existing -> {
              // Use mapper to update fields
              messageMapper.updateMessageFromMessageRequest(request, existing);

              return messageRepository.save(existing);
            })
        .map(messageMapper::toMessageResponse)
        .doOnNext(
            response -> {
              log.info("Message updated: {}", response.getId());
              // Emit the updated message to stream subscribers
              messageSink.tryEmitNext(response);
            })
        .doOnError(e -> log.error("Error updating message: {}", e.getMessage()));
  }

  @Override
  public Mono<Void> deleteMessageDirect(String messageId) {
    log.info("Direct: Deleting message: {}", messageId);

    return messageRepository
        .findById(messageId)
        .switchIfEmpty(Mono.error(new RuntimeException("Message not found with id: " + messageId)))
        .flatMap(
            message -> {
              // Create a deletion event response
              MessageResponse deletionEvent = messageMapper.toMessageResponse(message);
              deletionEvent.setDeleted(true);

              // Emit the deletion event to stream subscribers
              messageSink.tryEmitNext(deletionEvent);

              // Delete the message from database
              return messageRepository.delete(message);
            })
        .doOnSuccess(v -> log.info("Message deleted and deletion event emitted: {}", messageId))
        .doOnError(e -> log.error("Error deleting message: {}", e.getMessage()));
  }

  @Override
  public Mono<MessageResponse> getMessageByIdDirect(String messageId) {
    log.info("Direct: Getting message by id: {}", messageId);

    return messageRepository
        .findById(messageId)
        .switchIfEmpty(Mono.error(new RuntimeException("Message not found with id: " + messageId)))
        .map(messageMapper::toMessageResponse)
        .doOnError(e -> log.error("Error getting message: {}", e.getMessage()));
  }

  @Override
  public Flux<MessageResponse> getMessagesByRoomDirect(String roomId) {
    log.info("Direct: Getting messages for room: {}", roomId);

    return messageRepository
        .findByRoomIdOrderByCreatedAtAsc(roomId)
        .map(messageMapper::toMessageResponse)
        .doOnComplete(() -> log.info("Finished getting messages for room: {}", roomId))
        .doOnError(e -> log.error("Error getting messages for room: {}", e.getMessage()));
  }

  @Override
  public Flux<MessageResponse> streamMessagesDirect(String roomId) {
    log.info("Direct: Streaming messages for room: {}", roomId);

    // First, send existing messages
    Flux<MessageResponse> existingMessages = getMessagesByRoomDirect(roomId);

    // Then, stream new messages from sink filtered by roomId
    Flux<MessageResponse> newMessages =
        messageSink
            .asFlux()
            .filter(msg -> msg.getRoomId().equals(roomId))
            .doOnNext(msg -> log.debug("Streaming new message: {}", msg.getId()));

    // Combine both: existing + real-time
    return existingMessages.concatWith(newMessages);
  }
}
