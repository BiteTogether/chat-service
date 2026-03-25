package com.bitetogether.chat_service.service;

import com.bitetogether.chat_service.dto.message.ChatInboundMessage;
import com.bitetogether.chat_service.dto.message.ChatMessageDTO;
import com.bitetogether.chat_service.dto.message.MessagePageResponse;
import com.bitetogether.chat_service.dto.message.UpdateMessageRequest;
import com.bitetogether.chat_service.enums.MessageCreatedEvent;
import com.bitetogether.chat_service.enums.MessageDeletedEvent;
import com.bitetogether.chat_service.enums.MessageUpdatedEvent;
import com.bitetogether.chat_service.event.DomainEventPublisher;
import com.bitetogether.chat_service.exception.ErrorCode;
import com.bitetogether.chat_service.mapper.MessageMapper;
import com.bitetogether.chat_service.model.Message;
import com.bitetogether.chat_service.repository.ConversationRepository;
import com.bitetogether.chat_service.repository.MessageRepository;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import com.bitetogether.chat_service.util.PaginationUtils;
import com.bitetogether.chat_service.util.PaginationUtils.CursorPageResult;
import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.common.util.ApiResponseUtil;
import com.bitetogether.common.util.ReactiveUserContextUtils;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageService {

  private static final String USER_ID_NOT_FOUND_MSG = "User ID not found in context";

  SequenceService sequenceService;
  MessageRepository messageRepository;
  ParticipantRepository participantRepository;
  ConversationRepository conversationRepository;
  CryptoService cryptoService;
  MessageMapper mapper;
  DomainEventPublisher eventPublisher;

  // ==================== CREATE ====================

  /** Process incoming message from REST API (uses reactive context for user ID). */
  public Mono<ApiResponseDTO<ChatMessageDTO>> processIncoming(ChatInboundMessage inbound) {
    return ReactiveUserContextUtils.getUserIdOrError(USER_ID_NOT_FOUND_MSG)
        .flatMap(senderId -> processIncoming(inbound, senderId));
  }

  /** Process incoming message from WebSocket (senderId passed directly). */
  public Mono<ApiResponseDTO<ChatMessageDTO>> processIncoming(
      ChatInboundMessage inbound, Long senderId) {
    return validateMember(inbound.getConversationId(), senderId)
        .then(sequenceService.nextSeq(inbound.getConversationId()))
        .flatMap(seq -> buildMessage(inbound, senderId, seq))
        .flatMap(messageRepository::save)
        .flatMap(
            savedMessage ->
                updateConversationLastMessageTime(savedMessage).then(toDecryptedDTO(savedMessage)))
        .doOnNext(
            dto ->
                eventPublisher.publishMessageCreated(
                    new MessageCreatedEvent(dto.getConversationId(), dto)))
        .map(
            dto ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.CREATED, "Message sent successfully", dto));
  }

  /** Update the conversation's lastMessageTime and lastMessageId when a new message is sent. */
  private Mono<Void> updateConversationLastMessageTime(Message message) {
    return conversationRepository
        .findById(message.getConversationId())
        .flatMap(
            conversation -> {
              conversation.setLastMessageId(message.getId());
              conversation.setLastMessageTime(LocalDateTime.now());
              return conversationRepository.save(conversation);
            })
        .then();
  }

  // ==================== READ ====================

  /**
   * Get messages by conversation ID with cursor-based pagination. Messages are returned in
   * descending order (newest first). Automatically marks fetched messages as read for the current
   * user.
   *
   * @param conversationId the conversation ID
   * @param cursor the cursor (sequence number) to start from (exclusive), null for first page
   * @param limit the number of messages to fetch (default 20, max 100)
   * @return paginated messages response
   */
  public Mono<ApiResponseDTO<MessagePageResponse>> getMessagesByConversationId(
      String conversationId, Long cursor, Integer limit) {
    return ReactiveUserContextUtils.getUserIdOrError(USER_ID_NOT_FOUND_MSG)
        .flatMap(
            userId ->
                validateMember(conversationId, userId)
                    .then(fetchMessages(conversationId, cursor, limit))
                    .flatMap(
                        response ->
                            markAsRead(conversationId, userId, response.getData())
                                .thenReturn(response)));
  }

  /**
   * Mark messages as read up to the highest sequence number in the fetched messages. This is called
   * automatically when fetching messages.
   */
  private Mono<Void> markAsRead(String conversationId, Long userId, MessagePageResponse response) {
    if (response.getMessages() == null || response.getMessages().isEmpty()) {
      return Mono.empty();
    }

    // Get the highest sequence from fetched messages (first one since sorted desc)
    long highestSeq =
        response.getMessages().stream().mapToLong(ChatMessageDTO::getSeq).max().orElse(0L);

    if (highestSeq <= 0) {
      return Mono.empty();
    }

    return updateLastReadSequence(conversationId, userId, highestSeq);
  }

  /**
   * Update the last read sequence for a user in a conversation. Only updates if the new sequence is
   * higher than the current one.
   */
  private Mono<Void> updateLastReadSequence(
      String conversationId, Long userId, Long lastReadSequence) {
    return participantRepository
        .findByConversationIdAndUserId(conversationId, userId)
        .flatMap(
            participant -> {
              Long currentLastRead = participant.getLastReadMessageSequence();
              if (currentLastRead == null) {
                currentLastRead = 0L;
              }
              if (lastReadSequence > currentLastRead) {
                participant.setLastReadMessageSequence(lastReadSequence);
                return participantRepository.save(participant).then();
              }
              return Mono.empty();
            });
  }

  private Mono<ApiResponseDTO<MessagePageResponse>> fetchMessages(
      String conversationId, Long cursor, Integer limit) {
    PageRequest pageable = PaginationUtils.createPageRequest(limit);

    return (cursor == null
            ? messageRepository.findByConversationIdOrderBySequenceDesc(conversationId, pageable)
            : messageRepository.findByConversationIdAndSequenceLessThanOrderBySequenceDesc(
                conversationId, cursor, pageable))
        .flatMap(this::toDecryptedDTO)
        .collectList()
        .map(messages -> buildPageResponse(messages, limit));
  }

  private ApiResponseDTO<MessagePageResponse> buildPageResponse(
      java.util.List<ChatMessageDTO> messages, Integer requestedLimit) {
    CursorPageResult<ChatMessageDTO, Long> pageResult =
        PaginationUtils.buildCursorPageResult(messages, requestedLimit, ChatMessageDTO::getSeq);

    MessagePageResponse response =
        MessagePageResponse.builder()
            .messages(pageResult.getItems())
            .nextCursor(pageResult.getNextCursor())
            .hasMore(pageResult.isHasMore())
            .size(pageResult.getSize())
            .build();

    return ApiResponseUtil.buildApiResponse(
        ApiResponseStatus.SUCCESS, "Messages retrieved successfully", response);
  }

  /**
   * Get a single message by ID.
   *
   * @param messageId the message ID
   * @return the message
   */
  public Mono<ApiResponseDTO<ChatMessageDTO>> getMessageById(String messageId) {
    return ReactiveUserContextUtils.getUserIdOrError(USER_ID_NOT_FOUND_MSG)
        .flatMap(
            userId ->
                messageRepository
                    .findById(messageId)
                    .switchIfEmpty(Mono.error(new AppException(ErrorCode.MESSAGE_NOT_FOUND)))
                    .flatMap(
                        message ->
                            validateMember(message.getConversationId(), userId)
                                .then(toDecryptedDTO(message))))
        .map(
            dto ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.SUCCESS, "Message retrieved successfully", dto));
  }

  // ==================== UPDATE ====================

  /**
   * Update a message content. Only the sender can update their own message. Publishes a
   * MessageUpdatedEvent for real-time updates.
   *
   * @param messageId the message ID
   * @param request the update request containing new content
   * @return the updated message
   */
  public Mono<ApiResponseDTO<ChatMessageDTO>> updateMessage(
      String messageId, UpdateMessageRequest request) {
    return ReactiveUserContextUtils.getUserIdOrError(USER_ID_NOT_FOUND_MSG)
        .flatMap(
            userId ->
                messageRepository
                    .findById(messageId)
                    .switchIfEmpty(Mono.error(new AppException(ErrorCode.MESSAGE_NOT_FOUND)))
                    .flatMap(
                        message ->
                            validateMessageOwner(message, userId)
                                .then(validateMember(message.getConversationId(), userId))
                                .then(updateMessageContent(message, request.getContent()))));
  }

  private Mono<ApiResponseDTO<ChatMessageDTO>> updateMessageContent(
      Message message, String newContent) {
    return cryptoService
        .encrypt(newContent)
        .flatMap(
            encrypted -> {
              message.setCiphertext(encrypted.ciphertext());
              message.setIv(encrypted.iv());
              message.setAuthTag(encrypted.authTag());
              return messageRepository.save(message);
            })
        .flatMap(this::toDecryptedDTO)
        .doOnNext(
            dto ->
                eventPublisher.publishMessageUpdated(
                    new MessageUpdatedEvent(dto.getConversationId(), dto)))
        .map(
            dto ->
                ApiResponseUtil.buildApiResponse(
                    ApiResponseStatus.SUCCESS, "Message updated successfully", dto));
  }

  // ==================== DELETE ====================

  /**
   * Delete a message. Only the sender can delete their own message. Publishes a MessageDeletedEvent
   * for real-time updates.
   *
   * @param messageId the message ID
   * @return void
   */
  public Mono<ApiResponseDTO<Void>> deleteMessage(String messageId) {
    return ReactiveUserContextUtils.getUserIdOrError(USER_ID_NOT_FOUND_MSG)
        .flatMap(
            userId ->
                messageRepository
                    .findById(messageId)
                    .switchIfEmpty(Mono.error(new AppException(ErrorCode.MESSAGE_NOT_FOUND)))
                    .flatMap(
                        message ->
                            validateMessageOwner(message, userId)
                                .then(validateMember(message.getConversationId(), userId))
                                .then(deleteMessageAndPublish(message))));
  }

  private Mono<ApiResponseDTO<Void>> deleteMessageAndPublish(Message message) {
    String conversationId = message.getConversationId();
    String messageId = message.getId();

    return messageRepository
        .delete(message)
        .doOnSuccess(
            unused ->
                eventPublisher.publishMessageDeleted(
                    new MessageDeletedEvent(conversationId, messageId)))
        .then(
            Mono.fromCallable(
                () ->
                    ApiResponseUtil.buildApiResponse(
                        ApiResponseStatus.SUCCESS, "Message deleted successfully", null)));
  }

  // ==================== HELPERS ====================

  private Mono<ChatMessageDTO> toDecryptedDTO(Message message) {
    return cryptoService
        .decrypt(message.getCiphertext(), message.getIv(), message.getAuthTag())
        .map(decryptedContent -> mapper.toChatMessageDTO(message, decryptedContent));
  }

  private Mono<Void> validateMember(String conversationId, Long userId) {
    return participantRepository
        .existsByConversationIdAndUserId(conversationId, userId)
        .flatMap(
            exists ->
                Boolean.TRUE.equals(exists)
                    ? Mono.empty()
                    : Mono.error(new AccessDeniedException("Not a participant")));
  }

  private Mono<Void> validateMessageOwner(Message message, Long userId) {
    if (!message.getSenderId().equals(userId)) {
      return Mono.error(new AppException(ErrorCode.MESSAGE_UPDATE_UNAUTHORIZED));
    }
    return Mono.empty();
  }

  private Mono<Message> buildMessage(ChatInboundMessage inbound, Long senderId, Long seq) {

    return cryptoService
        .encrypt(inbound.getContent())
        .map(
            encrypted ->
                Message.builder()
                    .conversationId(inbound.getConversationId())
                    .sequence(seq)
                    .senderId(senderId)
                    .type(inbound.getMessageType())
                    .ciphertext(encrypted.ciphertext())
                    .iv(encrypted.iv())
                    .authTag(encrypted.authTag())
                    .build());
  }
}
