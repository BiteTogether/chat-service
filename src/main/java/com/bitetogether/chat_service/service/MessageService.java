package com.bitetogether.chat_service.service;

import com.bitetogether.chat_service.dto.message.ChatInboundMessage;
import com.bitetogether.chat_service.dto.message.ChatMessageDTO;
import com.bitetogether.chat_service.enums.MessageCreatedEvent;
import com.bitetogether.chat_service.event.DomainEventPublisher;
import com.bitetogether.chat_service.mapper.MessageMapper;
import com.bitetogether.chat_service.model.Message;
import com.bitetogether.chat_service.repository.MessageRepository;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.util.ApiResponseUtil;
import com.bitetogether.common.util.ReactiveUserContextUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageService {
  SequenceService sequenceService;
  MessageRepository messageRepository;
  ParticipantRepository participantRepository;
  CryptoService cryptoService;
  MessageMapper mapper;
  DomainEventPublisher eventPublisher;

  /**
   * Process incoming message from REST API (uses reactive context for user ID).
   */
  public Mono<ApiResponseDTO<ChatMessageDTO>> processIncoming(ChatInboundMessage inbound) {
    return ReactiveUserContextUtils.getUserIdOrError("Sender ID not found in context")
        .flatMap(senderId -> processIncoming(inbound, senderId));
  }

  /**
   * Process incoming message from WebSocket (senderId passed directly).
   */
  public Mono<ApiResponseDTO<ChatMessageDTO>> processIncoming(ChatInboundMessage inbound, Long senderId) {
    return validateMember(inbound.getConversationId(), senderId)
        .then(sequenceService.nextSeq(inbound.getConversationId()))
        .flatMap(seq -> buildMessage(inbound, senderId, seq))
        .flatMap(messageRepository::save)
        .flatMap(this::toDecryptedDTO)
        .doOnNext(dto -> eventPublisher.publishMessageCreated(
            new MessageCreatedEvent(dto.getConversationId(), dto)))
        .map(dto -> ApiResponseUtil.buildApiResponse(
            ApiResponseStatus.CREATED, "Message sent successfully", dto));
  }

  private Mono<ChatMessageDTO> toDecryptedDTO(Message message) {
    return cryptoService.decrypt(message.getCiphertext(), message.getIv(), message.getAuthTag())
        .map(decryptedContent -> mapper.toChatMessageDTO(message, decryptedContent));
  }


  private Mono<Void> validateMember(String conversationId, Long userId) {
    return participantRepository
        .existsByConversationIdAndUserId(conversationId, userId)
        .flatMap(
            exists ->
                exists ? Mono.empty() : Mono.error(new AccessDeniedException("Not a participant")));
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
