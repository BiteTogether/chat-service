package com.bitetogether.chat_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.chat_service.dto.message.ChatMessageDTO;
import com.bitetogether.chat_service.dto.message.SendMessageInbound;
import com.bitetogether.chat_service.dto.message.UpdateMessageRequest;
import com.bitetogether.chat_service.enums.crypto.EncryptedPayload;
import com.bitetogether.chat_service.enums.message.MessageType;
import com.bitetogether.chat_service.enums.websocket.WebSocketAction;
import com.bitetogether.chat_service.event.DomainEventPublisher;
import com.bitetogether.chat_service.exception.ErrorCode;
import com.bitetogether.chat_service.mapper.MessageMapper;
import com.bitetogether.chat_service.model.Conversation;
import com.bitetogether.chat_service.model.Message;
import com.bitetogether.chat_service.model.Participant;
import com.bitetogether.chat_service.repository.ConversationRepository;
import com.bitetogether.chat_service.repository.MessageRepository;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import com.bitetogether.common.dto.UserContext;
import com.bitetogether.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

  @Mock private SequenceService sequenceService;
  @Mock private MessageRepository messageRepository;
  @Mock private ParticipantRepository participantRepository;
  @Mock private ConversationRepository conversationRepository;
  @Mock private CryptoService cryptoService;
  @Mock private MessageMapper mapper;
  @Mock private DomainEventPublisher eventPublisher;

  private MessageService messageService;

  private static final UserContext USER_CONTEXT = new UserContext(1L, "USER", "a@b.com", "user1");

  @BeforeEach
  void setUp() {
    messageService =
        new MessageService(
            sequenceService,
            messageRepository,
            participantRepository,
            conversationRepository,
            cryptoService,
            mapper,
            eventPublisher);
  }

  private <T> Mono<T> withUser(Mono<T> mono) {
    return mono.contextWrite(ctx -> ctx.put("USER_CONTEXT", USER_CONTEXT));
  }

  @Test
  void processIncoming_WithValidInbound_ReturnsCreatedMessage() {
    // Arrange
    SendMessageInbound inbound =
        new SendMessageInbound(
            "conv_1", WebSocketAction.SEND, MessageType.TEXT, "hello", null, null);
    Long senderId = 1L;

    when(participantRepository.existsByConversationIdAndUserId("conv_1", senderId))
        .thenReturn(Mono.just(true));
    when(sequenceService.nextSeq("conv_1")).thenReturn(Mono.just(1L));
    when(cryptoService.encrypt("hello"))
        .thenReturn(
            Mono.just(new EncryptedPayload(new byte[] {1}, new byte[] {2}, new byte[] {3})));

    Message savedMessage =
        Message.builder()
            .id("msg_1")
            .conversationId("conv_1")
            .senderId(senderId)
            .sequence(1L)
            .type(MessageType.TEXT)
            .ciphertext(new byte[] {1})
            .iv(new byte[] {2})
            .authTag(new byte[] {3})
            .build();
    when(messageRepository.save(any(Message.class))).thenReturn(Mono.just(savedMessage));

    Conversation conversation = new Conversation();
    conversation.setId("conv_1");
    when(conversationRepository.findById("conv_1")).thenReturn(Mono.just(conversation));
    when(conversationRepository.save(any(Conversation.class))).thenReturn(Mono.just(conversation));
    when(cryptoService.decrypt(any(), any(), any())).thenReturn(Mono.just("hello"));

    ChatMessageDTO dto = new ChatMessageDTO();
    dto.setId("msg_1");
    dto.setConversationId("conv_1");
    dto.setContent("hello");
    when(mapper.toChatMessageDTO(any(Message.class), eq("hello"))).thenReturn(dto);

    // Act & Assert
    StepVerifier.create(messageService.processIncoming(inbound, senderId))
        .assertNext(
            response -> {
              assertEquals("hello", response.getData().getContent());
              assertEquals("msg_1", response.getData().getId());
            })
        .verifyComplete();

    verify(eventPublisher).publishMessageCreated(any());
  }

  @Test
  void processIncoming_WhenNotParticipant_ReturnsAccessDenied() {
    // Arrange
    SendMessageInbound inbound =
        new SendMessageInbound(
            "conv_1", WebSocketAction.SEND, MessageType.TEXT, "hello", null, null);

    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(false));
    when(sequenceService.nextSeq("conv_1")).thenReturn(Mono.just(1L));

    // Act & Assert
    StepVerifier.create(messageService.processIncoming(inbound, 1L))
        .expectError(org.springframework.security.access.AccessDeniedException.class)
        .verify();
  }

  @Test
  void processIncoming_PostComment_WithoutPostId_ReturnsError() {
    // Arrange
    SendMessageInbound inbound =
        new SendMessageInbound(
            "conv_1", WebSocketAction.SEND, MessageType.POST_COMMENT, "comment", null, null);

    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(sequenceService.nextSeq("conv_1")).thenReturn(Mono.just(1L));

    // Act & Assert
    StepVerifier.create(messageService.processIncoming(inbound, 1L))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.INVALID_MESSAGE_POST_ID)
        .verify();
  }

  @Test
  void processIncoming_PostComment_WithPostId_Succeeds() {
    // Arrange
    SendMessageInbound inbound =
        new SendMessageInbound(
            "conv_1", WebSocketAction.SEND, MessageType.POST_COMMENT, "comment", "post_1", null);

    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(sequenceService.nextSeq("conv_1")).thenReturn(Mono.just(2L));
    when(cryptoService.encrypt("comment"))
        .thenReturn(
            Mono.just(new EncryptedPayload(new byte[] {1}, new byte[] {2}, new byte[] {3})));

    Message savedMessage =
        Message.builder()
            .id("msg_2")
            .conversationId("conv_1")
            .senderId(1L)
            .sequence(2L)
            .type(MessageType.POST_COMMENT)
            .postId("post_1")
            .ciphertext(new byte[] {1})
            .iv(new byte[] {2})
            .authTag(new byte[] {3})
            .build();
    when(messageRepository.save(any(Message.class))).thenReturn(Mono.just(savedMessage));

    Conversation conversation = new Conversation();
    conversation.setId("conv_1");
    when(conversationRepository.findById("conv_1")).thenReturn(Mono.just(conversation));
    when(conversationRepository.save(any())).thenReturn(Mono.just(conversation));
    when(cryptoService.decrypt(any(), any(), any())).thenReturn(Mono.just("comment"));

    ChatMessageDTO dto = new ChatMessageDTO();
    dto.setId("msg_2");
    dto.setContent("comment");
    dto.setConversationId("conv_1");
    when(mapper.toChatMessageDTO(any(Message.class), eq("comment"))).thenReturn(dto);

    // Act & Assert
    StepVerifier.create(messageService.processIncoming(inbound, 1L))
        .assertNext(response -> assertEquals("comment", response.getData().getContent()))
        .verifyComplete();
  }

  @Test
  void getMessageById_WhenNotFound_ReturnsError() {
    // Arrange
    when(messageRepository.findById("msg_x")).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(withUser(messageService.getMessageById("msg_x")))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.MESSAGE_NOT_FOUND)
        .verify();
  }

  @Test
  void getMessageById_WhenFound_ReturnsDecrypted() {
    // Arrange
    Message message =
        Message.builder()
            .id("msg_1")
            .conversationId("conv_1")
            .senderId(1L)
            .sequence(1L)
            .ciphertext(new byte[] {1})
            .iv(new byte[] {2})
            .authTag(new byte[] {3})
            .build();

    when(messageRepository.findById("msg_1")).thenReturn(Mono.just(message));
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(cryptoService.decrypt(any(), any(), any())).thenReturn(Mono.just("decrypted"));

    ChatMessageDTO dto = new ChatMessageDTO();
    dto.setId("msg_1");
    dto.setContent("decrypted");
    when(mapper.toChatMessageDTO(any(Message.class), eq("decrypted"))).thenReturn(dto);

    // Act & Assert
    StepVerifier.create(withUser(messageService.getMessageById("msg_1")))
        .assertNext(resp -> assertEquals("decrypted", resp.getData().getContent()))
        .verifyComplete();
  }

  @Test
  void updateMessage_WhenNotOwner_ReturnsUnauthorized() {
    // Arrange
    Message message = Message.builder().id("msg_1").conversationId("conv_1").senderId(99L).build();

    when(messageRepository.findById("msg_1")).thenReturn(Mono.just(message));
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(cryptoService.encrypt("new content"))
        .thenReturn(
            Mono.just(new EncryptedPayload(new byte[] {1}, new byte[] {2}, new byte[] {3})));

    UpdateMessageRequest request = new UpdateMessageRequest();
    request.setContent("new content");

    // Act & Assert
    StepVerifier.create(withUser(messageService.updateMessage("msg_1", request)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.MESSAGE_UPDATE_UNAUTHORIZED)
        .verify();
  }

  @Test
  void updateMessage_WhenOwner_UpdatesSuccessfully() {
    // Arrange
    Message message =
        Message.builder()
            .id("msg_1")
            .conversationId("conv_1")
            .senderId(1L)
            .ciphertext(new byte[] {1})
            .iv(new byte[] {2})
            .authTag(new byte[] {3})
            .build();

    when(messageRepository.findById("msg_1")).thenReturn(Mono.just(message));
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(cryptoService.encrypt("new content"))
        .thenReturn(
            Mono.just(new EncryptedPayload(new byte[] {4}, new byte[] {5}, new byte[] {6})));
    when(messageRepository.save(any(Message.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    when(cryptoService.decrypt(any(), any(), any())).thenReturn(Mono.just("new content"));

    ChatMessageDTO dto = new ChatMessageDTO();
    dto.setId("msg_1");
    dto.setContent("new content");
    dto.setConversationId("conv_1");
    when(mapper.toChatMessageDTO(any(Message.class), eq("new content"))).thenReturn(dto);

    UpdateMessageRequest request = new UpdateMessageRequest();
    request.setContent("new content");

    // Act & Assert
    StepVerifier.create(withUser(messageService.updateMessage("msg_1", request)))
        .assertNext(resp -> assertEquals("new content", resp.getData().getContent()))
        .verifyComplete();

    verify(eventPublisher).publishMessageUpdated(any());
  }

  @Test
  void deleteMessage_WhenOwner_DeletesSuccessfully() {
    // Arrange
    Message message = Message.builder().id("msg_1").conversationId("conv_1").senderId(1L).build();

    when(messageRepository.findById("msg_1")).thenReturn(Mono.just(message));
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(messageRepository.delete(any(Message.class))).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(withUser(messageService.deleteMessage("msg_1")))
        .assertNext(resp -> assertNull(resp.getData()))
        .verifyComplete();

    verify(eventPublisher).publishMessageDeleted(any());
  }

  @Test
  void deleteMessage_WhenNotFound_ReturnsError() {
    // Arrange
    when(messageRepository.findById("msg_x")).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(withUser(messageService.deleteMessage("msg_x")))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.MESSAGE_NOT_FOUND)
        .verify();
  }

  @Test
  void getMessagesByConversationId_ReturnsPagedMessages() {
    // Arrange
    Message message =
        Message.builder()
            .id("msg_1")
            .conversationId("conv_1")
            .senderId(1L)
            .sequence(5L)
            .ciphertext(new byte[] {1})
            .iv(new byte[] {2})
            .authTag(new byte[] {3})
            .build();

    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(messageRepository.findByConversationIdOrderBySequenceDesc(
            eq("conv_1"), any(PageRequest.class)))
        .thenReturn(Flux.just(message));
    when(cryptoService.decrypt(any(), any(), any())).thenReturn(Mono.just("hello"));

    ChatMessageDTO dto = new ChatMessageDTO();
    dto.setId("msg_1");
    dto.setContent("hello");
    dto.setSeq(5L);
    dto.setConversationId("conv_1");
    when(mapper.toChatMessageDTO(any(Message.class), eq("hello"))).thenReturn(dto);

    Participant participant = new Participant();
    participant.setLastReadMessageSequence(3L);
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(participant));
    when(participantRepository.save(any(Participant.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

    // Act & Assert
    StepVerifier.create(withUser(messageService.getMessagesByConversationId("conv_1", null, 20)))
        .assertNext(
            resp -> {
              assertEquals(1, resp.getData().getMessages().size());
              assertFalse(resp.getData().isHasMore());
            })
        .verifyComplete();
  }
}
