package com.bitetogether.chat_service.service.impl;

import com.bitetogether.chat_service.client.feign.UserClient;
import com.bitetogether.chat_service.dto.SenderInfo;
import com.bitetogether.chat_service.dto.request.MessageRequest;
import com.bitetogether.chat_service.dto.response.MessageResponse;
import com.bitetogether.chat_service.exception.ErrorCode;
import com.bitetogether.chat_service.mapper.MessageMapper;
import com.bitetogether.chat_service.mapper.SenderInfoMapper;
import com.bitetogether.chat_service.model.Message;
import com.bitetogether.chat_service.repository.MessageRepository;
import com.bitetogether.chat_service.service.MessageService;
import com.bitetogether.chat_service.service.RoomService;
import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.dto.ApiResponsePaginationDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.common.util.ApiResponseUtil;
import com.bitetogether.common.util.UserContextUtils;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
  RoomService roomService;
  UserClient userClient;
  SenderInfoMapper senderInfoMapper;

  Sinks.Many<MessageResponse> messageSink = Sinks.many().multicast().onBackpressureBuffer();

  @Override
  public Mono<ApiResponseDTO<MessageResponse>> sendMessage(MessageRequest request) {
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
  public Mono<ApiResponseDTO<MessageResponse>> updateMessage(
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
  public Mono<ApiResponseDTO<Void>> deleteMessage(String messageId) {
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
  public Mono<ApiResponseDTO<MessageResponse>> getMessageById(String messageId) {
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
  public Mono<ApiResponsePaginationDTO<MessageResponse>> getMessagesByRoomPaginated(
      String roomId, int page, int size) {
    log.info(
        "REST: Getting paginated messages for room: {} (page: {}, size: {})", roomId, page, size);

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));

    // Get total count and content in parallel
    Mono<Long> totalMono = messageRepository.countByRoomId(roomId);
    Mono<List<MessageResponse>> contentMono =
        messageRepository
            .findByRoomId(roomId, pageable)
            .flatMap(this::enrichMessageWithSenderAndReplyContext)
            .collectList();

    return Mono.zip(totalMono, contentMono)
        .map(
            tuple -> {
              long total = tuple.getT1();
              List<MessageResponse> content = tuple.getT2();
              int totalPages = (int) Math.ceil((double) total / size);

              return ApiResponseUtil.buildApiResponse(
                  ApiResponseStatus.SUCCESS,
                  "Messages retrieved successfully",
                  content,
                  page,
                  totalPages,
                  total);
            })
        .onErrorResume(
            e -> {
              log.error("Failed to get paginated messages: {}", e.getMessage());
              return Mono.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST,
                      "Failed to fetch messages: " + e.getMessage(),
                      java.util.Collections.emptyList(),
                      page,
                      0,
                      0L));
            });
  }

  @Override
  public Flux<ApiResponseDTO<MessageResponse>> streamMessages(String roomId) {
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

  @Override
  public Flux<ApiResponseDTO<MessageResponse>> getMessageReplies(String messageId) {
    log.info("REST: Getting replies for message: {}", messageId);
    return getMessageRepliesDirect(messageId)
        .map(
            response ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.SUCCESS, "Replies retrieved successfully", response))
        .onErrorResume(
            e -> {
              log.error("Failed to get replies: {}", e.getMessage());
              return Flux.just(
                  ApiResponseUtil.buildApiResponse(
                      ApiResponseStatus.BAD_REQUEST,
                      "Failed to fetch replies: " + e.getMessage(),
                      null));
            });
  }

  @Override
  public Mono<MessageResponse> sendMessageDirect(MessageRequest request) {
    log.info("Direct: Sending message to room: {}", request.getRoomId());
    Message message = messageMapper.toMessage(request);

    Long currentUserId = UserContextUtils.getCurrentUserId();
    message.setSenderId(currentUserId);

    // Emit to sink for real-time streaming
    return messageRepository
        .save(message)
        .flatMap(this::enrichMessageWithSenderAndReplyContext)
        .flatMap(
            response -> {
              log.info("Message saved with id: {}", response.getId());
              // Update the room's last message timestamp
              return roomService
                  .updateLastMessage(response.getRoomId(), response.getId(), LocalDateTime.now())
                  .thenReturn(response);
            })
        .doOnNext(messageSink::tryEmitNext)
        .doOnError(e -> log.error("Error saving message: {}", e.getMessage()));
  }

  @Override
  public Mono<MessageResponse> updateMessageDirect(String messageId, MessageRequest request) {
    log.info("Direct: Updating message: {}", messageId);

    Long currentUserId = UserContextUtils.getCurrentUserId();

    return messageRepository
        .findById(messageId)
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.MESSAGE_NOT_FOUND)))
        .flatMap(
            existing -> {
              if (!existing.getSenderId().equals(currentUserId)) {
                log.warn(
                    "User {} attempted to update message {} owned by user {}",
                    currentUserId,
                    messageId,
                    existing.getSenderId());
                return Mono.error(new AppException(ErrorCode.MESSAGE_UPDATE_UNAUTHORIZED));
              }
              messageMapper.updateMessageFromMessageRequest(request, existing);
              return messageRepository.save(existing);
            })
        .flatMap(this::enrichMessageWithSenderAndReplyContext)
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

    Long currentUserId = UserContextUtils.getCurrentUserId();

    return messageRepository
        .findById(messageId)
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.MESSAGE_NOT_FOUND)))
        .flatMap(
            message -> {
              if (!message.getSenderId().equals(currentUserId)) {
                log.warn(
                    "User {} attempted to delete message {} owned by user {}",
                    currentUserId,
                    messageId,
                    message.getSenderId());
                return Mono.error(new AppException(ErrorCode.MESSAGE_DELETE_UNAUTHORIZED));
              }
              MessageResponse deletionEvent = messageMapper.toMessageResponse(message);
              deletionEvent.setDeleted(true);

              // Emit the deletion event to stream subscribers
              messageSink.tryEmitNext(deletionEvent);

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
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.MESSAGE_NOT_FOUND)))
        .flatMap(this::enrichMessageWithSenderAndReplyContext)
        .doOnError(e -> log.error("Error getting message: {}", e.getMessage()));
  }

  @Override
  public Flux<MessageResponse> getMessagesByRoomDirect(String roomId) {
    log.info("Direct: Getting messages for room: {}", roomId);

    return messageRepository
        .findByRoomIdOrderByCreatedAtAsc(roomId)
        .flatMap(this::enrichMessageWithSenderAndReplyContext)
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

  @Override
  public Flux<MessageResponse> getMessageRepliesDirect(String messageId) {
    log.info("Direct: Getting replies for message: {}", messageId);

    return messageRepository
        .findByReplyToMessageId(messageId)
        .flatMap(this::enrichMessageWithSenderAndReplyContext)
        .doOnComplete(() -> log.info("Finished getting replies for message: {}", messageId))
        .doOnError(e -> log.error("Error getting replies: {}", e.getMessage()));
  }

  private Mono<MessageResponse> enrichMessageWithSenderAndReplyContext(Message message) {
    MessageResponse response = messageMapper.toMessageResponse(message);

    // Use reactive WebClient with bearer token forwarding from security context
    Mono<SenderInfo> senderInfoMono =
        userClient
            .getUserById(message.getSenderId())
            .map(senderInfoMapper::senderInfoFromUserDto)
            .doOnNext(response::setSender)
            .onErrorResume(
                e -> {
                  log.warn(
                      "Could not fetch sender info for user {}: {}",
                      message.getSenderId(),
                      e.getMessage());
                  return Mono.empty();
                });

    return senderInfoMono.then(
        enrichReplyContext(message, response)); // reuse your reply-context logic
  }

  private Mono<MessageResponse> enrichReplyContext(Message message, MessageResponse response) {
    if (message.getReplyToMessageId() != null && !message.getReplyToMessageId().isEmpty()) {
      return messageRepository
          .findById(message.getReplyToMessageId())
          .map(messageMapper::toMessageResponse)
          .doOnNext(response::setReplyTo)
          .thenReturn(response)
          .onErrorResume(
              e -> {
                log.warn(
                    "Could not fetch reply context for message {}: {}",
                    message.getId(),
                    e.getMessage());
                return Mono.just(response);
              });
    }
    return Mono.just(response);
  }
}
