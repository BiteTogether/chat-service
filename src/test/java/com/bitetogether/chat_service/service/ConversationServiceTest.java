package com.bitetogether.chat_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bitetogether.chat_service.dto.conversation.ConversationDTO;
import com.bitetogether.chat_service.dto.conversation.CreateConversationRequest;
import com.bitetogether.chat_service.dto.conversation.ParticipantDTO;
import com.bitetogether.chat_service.dto.conversation.UpdateConversationRequest;
import com.bitetogether.chat_service.enums.conversation.ConversationType;
import com.bitetogether.chat_service.enums.conversation.Role;
import com.bitetogether.chat_service.exception.ErrorCode;
import com.bitetogether.chat_service.mapper.ConversationMapper;
import com.bitetogether.chat_service.mapper.MessageMapper;
import com.bitetogether.chat_service.model.Conversation;
import com.bitetogether.chat_service.model.Participant;
import com.bitetogether.chat_service.repository.ChatUserSnapshotRepository;
import com.bitetogether.chat_service.repository.ConversationRepository;
import com.bitetogether.chat_service.repository.MessageRepository;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import com.bitetogether.common.dto.UserContext;
import com.bitetogether.common.exception.AppException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

  @Mock private ConversationRepository conversationRepository;
  @Mock private ParticipantRepository participantRepository;
  @Mock private MessageRepository messageRepository;
  @Mock private ChatUserSnapshotRepository chatUserSnapshotRepository;
  @Mock private ConversationMapper conversationMapper;
  @Mock private MessageMapper messageMapper;
  @Mock private CryptoService cryptoService;
  @Mock private LiveLocationService liveLocationService;
  @Mock private FirebaseStorageService firebaseStorageService;

  private ConversationService conversationService;

  private static final UserContext USER_CONTEXT = new UserContext(1L, "USER", "a@b.com", "user1");

  @BeforeEach
  void setUp() {
    conversationService =
        new ConversationService(
            conversationRepository,
            participantRepository,
            messageRepository,
            chatUserSnapshotRepository,
            conversationMapper,
            messageMapper,
            cryptoService,
            liveLocationService,
            firebaseStorageService);

    // Default lenient stubs to prevent NPE from eager .then() evaluation
    lenient().when(conversationRepository.findById(anyString())).thenReturn(Mono.empty());
    lenient().when(conversationRepository.save(any(Conversation.class))).thenReturn(Mono.empty());
    lenient().when(participantRepository.findByUserId(anyLong())).thenReturn(Flux.empty());
    lenient()
        .when(participantRepository.findByConversationId(anyString()))
        .thenReturn(Flux.empty());
    lenient()
        .when(participantRepository.existsByConversationIdAndUserId(anyString(), anyLong()))
        .thenReturn(Mono.just(false));
    lenient()
        .when(participantRepository.findByConversationIdAndUserId(anyString(), anyLong()))
        .thenReturn(Mono.empty());
    lenient().when(participantRepository.save(any(Participant.class))).thenReturn(Mono.empty());
    lenient()
        .when(participantRepository.deleteByConversationId(anyString()))
        .thenReturn(Mono.empty());
    lenient()
        .when(participantRepository.deleteByConversationIdAndUserId(anyString(), anyLong()))
        .thenReturn(Mono.empty());
    lenient()
        .when(participantRepository.countByConversationIdAndRole(anyString(), any(Role.class)))
        .thenReturn(Mono.just(0L));
    lenient()
        .when(messageRepository.findTopByConversationIdOrderBySequenceDesc(anyString()))
        .thenReturn(Mono.empty());
    lenient()
        .when(messageRepository.countByConversationIdAndSequenceGreaterThan(anyString(), anyLong()))
        .thenReturn(Mono.just(0L));
    lenient().when(messageRepository.deleteByConversationId(anyString())).thenReturn(Mono.empty());
  }

  private <T> Mono<T> withUser(Mono<T> mono) {
    return mono.contextWrite(ctx -> ctx.put("USER_CONTEXT", USER_CONTEXT));
  }

  // ==================== CREATE ====================

  @Test
  void createConversation_GroupWithName_CreatesSuccessfully() {
    // Arrange
    CreateConversationRequest request = new CreateConversationRequest();
    request.setType(ConversationType.GROUP);
    request.setName("Team Chat");
    request.setParticipantIds(Set.of(2L, 3L));

    Conversation savedConversation = new Conversation();
    savedConversation.setId("conv_1");
    savedConversation.setType(ConversationType.GROUP);
    savedConversation.setName("Team Chat");

    when(conversationRepository.save(any(Conversation.class)))
        .thenReturn(Mono.just(savedConversation));
    when(participantRepository.save(any(Participant.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    when(conversationRepository.findById("conv_1")).thenReturn(Mono.just(savedConversation));
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(participantRepository.findByConversationId("conv_1")).thenReturn(Flux.empty());
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(createParticipant("conv_1", 1L, Role.ADMIN)));
    when(messageRepository.countByConversationIdAndSequenceGreaterThan(eq("conv_1"), anyLong()))
        .thenReturn(Mono.just(0L));
    when(messageRepository.findTopByConversationIdOrderBySequenceDesc("conv_1"))
        .thenReturn(Mono.empty());

    ConversationDTO dto = new ConversationDTO();
    dto.setId("conv_1");
    dto.setType(ConversationType.GROUP);
    dto.setName("Team Chat");
    when(conversationMapper.toDTO(any(Conversation.class), any(), anyLong(), any()))
        .thenReturn(dto);

    // Act & Assert
    StepVerifier.create(withUser(conversationService.createConversation(request)))
        .assertNext(
            response -> {
              assertNotNull(response.getData());
              assertEquals("conv_1", response.getData().getId());
              assertEquals("Team Chat", response.getData().getName());
            })
        .verifyComplete();
  }

  @Test
  void createConversation_GroupWithoutName_ReturnsError() {
    // Arrange
    CreateConversationRequest request = new CreateConversationRequest();
    request.setType(ConversationType.GROUP);
    request.setName("");
    request.setParticipantIds(Set.of(2L, 3L));

    // Act & Assert
    StepVerifier.create(withUser(conversationService.createConversation(request)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode()
                        == ErrorCode.GROUP_CONVERSATION_REQUIRES_NAME)
        .verify();
  }

  @Test
  void createConversation_DirectWithTooManyParticipants_ReturnsError() {
    // Arrange
    CreateConversationRequest request = new CreateConversationRequest();
    request.setType(ConversationType.DIRECT);
    request.setParticipantIds(Set.of(2L, 3L)); // + user1 = 3 participants

    // Act & Assert
    StepVerifier.create(withUser(conversationService.createConversation(request)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode()
                        == ErrorCode.DIRECT_CONVERSATION_REQUIRES_TWO_PARTICIPANTS)
        .verify();
  }

  @Test
  void createConversation_DirectWithOneOtherUser_CreatesSuccessfully() {
    // Arrange
    CreateConversationRequest request = new CreateConversationRequest();
    request.setType(ConversationType.DIRECT);
    request.setParticipantIds(Set.of(2L));

    // No existing conversation
    when(participantRepository.findByUserId(any(Long.class))).thenReturn(Flux.empty());

    Conversation savedConversation = new Conversation();
    savedConversation.setId("conv_1");
    savedConversation.setType(ConversationType.DIRECT);

    when(conversationRepository.save(any(Conversation.class)))
        .thenReturn(Mono.just(savedConversation));
    when(participantRepository.save(any(Participant.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    when(conversationRepository.findById("conv_1")).thenReturn(Mono.just(savedConversation));
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(participantRepository.findByConversationId("conv_1")).thenReturn(Flux.empty());
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(createParticipant("conv_1", 1L, Role.ADMIN)));
    when(messageRepository.countByConversationIdAndSequenceGreaterThan(eq("conv_1"), anyLong()))
        .thenReturn(Mono.just(0L));
    when(messageRepository.findTopByConversationIdOrderBySequenceDesc("conv_1"))
        .thenReturn(Mono.empty());

    ConversationDTO dto = new ConversationDTO();
    dto.setId("conv_1");
    dto.setType(ConversationType.DIRECT);
    when(conversationMapper.toDTO(any(Conversation.class), any(), anyLong(), any()))
        .thenReturn(dto);

    // Act & Assert
    StepVerifier.create(withUser(conversationService.createConversation(request)))
        .assertNext(
            response -> {
              assertNotNull(response.getData());
              assertEquals("conv_1", response.getData().getId());
            })
        .verifyComplete();
  }

  // ==================== READ ====================

  @Test
  void getConversationById_WhenNotParticipant_ReturnsError() {
    // Arrange
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(false));

    // Act & Assert
    StepVerifier.create(withUser(conversationService.getConversationById("conv_1")))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.NOT_A_PARTICIPANT)
        .verify();
  }

  @Test
  void getConversationById_WhenNotFound_ReturnsError() {
    // Arrange
    when(participantRepository.existsByConversationIdAndUserId("conv_x", 1L))
        .thenReturn(Mono.just(true));
    when(conversationRepository.findById("conv_x")).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(withUser(conversationService.getConversationById("conv_x")))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.CONVERSATION_NOT_FOUND)
        .verify();
  }

  @Test
  void getConversationById_WhenFound_ReturnsDTO() {
    // Arrange
    Conversation conversation = new Conversation();
    conversation.setId("conv_1");
    conversation.setType(ConversationType.GROUP);
    conversation.setName("Team");

    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(conversationRepository.findById("conv_1")).thenReturn(Mono.just(conversation));
    when(participantRepository.findByConversationId("conv_1")).thenReturn(Flux.empty());
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(createParticipant("conv_1", 1L, Role.ADMIN)));
    when(messageRepository.countByConversationIdAndSequenceGreaterThan(eq("conv_1"), anyLong()))
        .thenReturn(Mono.just(3L));
    when(messageRepository.findTopByConversationIdOrderBySequenceDesc("conv_1"))
        .thenReturn(Mono.empty());

    ConversationDTO dto = new ConversationDTO();
    dto.setId("conv_1");
    dto.setName("Team");
    dto.setUnreadCount(3L);
    when(conversationMapper.toDTO(any(Conversation.class), any(), eq(3L), any())).thenReturn(dto);

    // Act & Assert
    StepVerifier.create(withUser(conversationService.getConversationById("conv_1")))
        .assertNext(
            response -> {
              assertEquals("conv_1", response.getData().getId());
              assertEquals(3L, response.getData().getUnreadCount());
            })
        .verifyComplete();
  }

  @Test
  void getMyConversations_WhenNoConversations_ReturnsEmptyPage() {
    // Arrange
    when(participantRepository.findByUserId(1L)).thenReturn(Flux.empty());

    // Act & Assert
    StepVerifier.create(withUser(conversationService.getMyConversations(null, 20)))
        .assertNext(
            response -> {
              assertEquals(0, response.getData().getSize());
              assertTrue(response.getData().getConversations().isEmpty());
            })
        .verifyComplete();
  }

  @Test
  void getDirectConversationIdWithCurrentUser_WhenNullUserId_ReturnsError() {
    // Act & Assert
    StepVerifier.create(withUser(conversationService.getDirectConversationIdWithCurrentUser(null)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode()
                        == ErrorCode.INVALID_DIRECT_CONVERSATION_USER_IDS)
        .verify();
  }

  @Test
  void getDirectConversationIdWithCurrentUser_WhenNotFound_ReturnsError() {
    // Arrange
    when(participantRepository.findByUserId(1L)).thenReturn(Flux.empty());

    // Act & Assert
    StepVerifier.create(withUser(conversationService.getDirectConversationIdWithCurrentUser(2L)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.CONVERSATION_NOT_FOUND)
        .verify();
  }

  @Test
  void getDirectConversationIdsBatch_WithEmptyList_ReturnsError() {
    // Act & Assert
    StepVerifier.create(withUser(conversationService.getDirectConversationIdsBatch(List.of())))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode()
                        == ErrorCode.INVALID_DIRECT_CONVERSATION_USER_IDS)
        .verify();
  }

  @Test
  void getDirectConversationIdsBatch_WithNull_ReturnsError() {
    // Act & Assert
    StepVerifier.create(withUser(conversationService.getDirectConversationIdsBatch(null)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode()
                        == ErrorCode.INVALID_DIRECT_CONVERSATION_USER_IDS)
        .verify();
  }

  @Test
  void getDirectConversationIdsBatch_WithOnlyCurrentUser_ReturnsEmptyMap() {
    // Act & Assert - all target user IDs are the current user
    StepVerifier.create(withUser(conversationService.getDirectConversationIdsBatch(List.of(1L))))
        .assertNext(
            response -> {
              // The item is created but conversationId is null since there's no mapping
              assertEquals(1, response.getData().getItems().size());
              assertEquals(1L, response.getData().getItems().get(0).getUserId());
            })
        .verifyComplete();
  }

  // ==================== UPDATE ====================

  @Test
  void updateConversation_WhenNotAdmin_ReturnsError() {
    // Arrange
    Participant memberParticipant = createParticipant("conv_1", 1L, Role.MEMBER);
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(memberParticipant));

    UpdateConversationRequest request = new UpdateConversationRequest();
    request.setName("New Name");

    // Act & Assert
    StepVerifier.create(withUser(conversationService.updateConversation("conv_1", request)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode()
                        == ErrorCode.CONVERSATION_UPDATE_UNAUTHORIZED)
        .verify();
  }

  @Test
  void updateConversation_WhenNotParticipant_ReturnsError() {
    // Arrange
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.empty());

    UpdateConversationRequest request = new UpdateConversationRequest();
    request.setName("New Name");

    // Act & Assert
    StepVerifier.create(withUser(conversationService.updateConversation("conv_1", request)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.NOT_A_PARTICIPANT)
        .verify();
  }

  // ==================== PARTICIPANT MANAGEMENT ====================

  @Test
  void addParticipant_WhenNotAdmin_ReturnsError() {
    // Arrange
    Participant memberParticipant = createParticipant("conv_1", 1L, Role.MEMBER);
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(memberParticipant));

    // Act & Assert
    StepVerifier.create(withUser(conversationService.addParticipant("conv_1", List.of(5L))))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode()
                        == ErrorCode.CONVERSATION_UPDATE_UNAUTHORIZED)
        .verify();
  }

  @Test
  void addParticipant_WithEmptyList_ReturnsError() {
    // Act & Assert
    StepVerifier.create(withUser(conversationService.addParticipant("conv_1", List.of())))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode()
                        == ErrorCode.INVALID_DIRECT_CONVERSATION_USER_IDS)
        .verify();
  }

  @Test
  void addParticipant_WithValidUsers_AddsSuccessfully() {
    // Arrange
    Participant adminParticipant = createParticipant("conv_1", 1L, Role.ADMIN);
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(adminParticipant));

    Participant existing = createParticipant("conv_1", 2L, Role.MEMBER);
    when(participantRepository.findByConversationId("conv_1"))
        .thenReturn(Flux.just(adminParticipant, existing));
    when(participantRepository.save(any(Participant.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    when(chatUserSnapshotRepository.findById(anyLong())).thenReturn(Mono.empty());

    ParticipantDTO participantDTO = new ParticipantDTO();
    participantDTO.setId("p_new");
    participantDTO.setRole(Role.MEMBER);
    when(conversationMapper.toParticipantDTO(any(Participant.class))).thenReturn(participantDTO);

    // Act & Assert - add user 5 (new) and user 2 (existing, should be skipped)
    StepVerifier.create(withUser(conversationService.addParticipant("conv_1", List.of(5L, 2L))))
        .assertNext(
            response -> {
              assertEquals(1, response.getData().getAddedParticipants().size());
              assertEquals(1, response.getData().getSkippedUserIds().size());
              assertTrue(response.getData().getSkippedUserIds().contains(2L));
            })
        .verifyComplete();
  }

  @Test
  void removeParticipant_WhenLastAdmin_ReturnsError() {
    // Arrange
    Participant adminParticipant = createParticipant("conv_1", 1L, Role.ADMIN);
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(adminParticipant));
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(participantRepository.countByConversationIdAndRole("conv_1", Role.ADMIN))
        .thenReturn(Mono.just(1L));

    // Act & Assert - user removing themselves
    StepVerifier.create(withUser(conversationService.removeParticipant("conv_1", 1L)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.CANNOT_REMOVE_LAST_ADMIN)
        .verify();
  }

  @Test
  void removeParticipant_WhenNotLastAdmin_RemovesSuccessfully() {
    // Arrange
    Participant adminParticipant = createParticipant("conv_1", 1L, Role.ADMIN);
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(adminParticipant));
    when(participantRepository.existsByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(true));
    when(participantRepository.countByConversationIdAndRole("conv_1", Role.ADMIN))
        .thenReturn(Mono.just(2L));
    when(participantRepository.deleteByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(withUser(conversationService.removeParticipant("conv_1", 1L)))
        .assertNext(response -> assertNotNull(response))
        .verifyComplete();
  }

  @Test
  void removeParticipant_WhenMember_RemovesWithoutAdminCheck() {
    // Arrange - user 1 (admin) removing user 5 (member)
    Participant adminParticipant = createParticipant("conv_1", 1L, Role.ADMIN);
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(adminParticipant));

    Participant memberToRemove = createParticipant("conv_1", 5L, Role.MEMBER);
    when(participantRepository.findByConversationIdAndUserId("conv_1", 5L))
        .thenReturn(Mono.just(memberToRemove));
    when(participantRepository.deleteByConversationIdAndUserId("conv_1", 5L))
        .thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(withUser(conversationService.removeParticipant("conv_1", 5L)))
        .assertNext(response -> assertNotNull(response))
        .verifyComplete();
  }

  @Test
  void updateParticipantRole_WhenDemotingLastAdmin_ReturnsError() {
    // Arrange
    Participant adminParticipant = createParticipant("conv_1", 1L, Role.ADMIN);
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(adminParticipant));
    when(participantRepository.countByConversationIdAndRole("conv_1", Role.ADMIN))
        .thenReturn(Mono.just(1L));

    // Act & Assert
    StepVerifier.create(
            withUser(conversationService.updateParticipantRole("conv_1", 1L, Role.MEMBER)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.CANNOT_REMOVE_LAST_ADMIN)
        .verify();
  }

  @Test
  void updateParticipantRole_WhenPromotingToAdmin_Succeeds() {
    // Arrange
    Participant adminCaller = createParticipant("conv_1", 1L, Role.ADMIN);
    Participant memberTarget = createParticipant("conv_1", 5L, Role.MEMBER);

    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(adminCaller));
    when(participantRepository.findByConversationIdAndUserId("conv_1", 5L))
        .thenReturn(Mono.just(memberTarget));
    when(participantRepository.save(any(Participant.class)))
        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    when(chatUserSnapshotRepository.findById(5L)).thenReturn(Mono.empty());

    ParticipantDTO participantDTO = new ParticipantDTO();
    participantDTO.setId("p_5");
    participantDTO.setRole(Role.ADMIN);
    when(conversationMapper.toParticipantDTO(any(Participant.class))).thenReturn(participantDTO);

    // Act & Assert
    StepVerifier.create(
            withUser(conversationService.updateParticipantRole("conv_1", 5L, Role.ADMIN)))
        .assertNext(response -> assertNotNull(response.getData()))
        .verifyComplete();
  }

  @Test
  void updateParticipantRole_WhenTargetNotFound_ReturnsError() {
    // Arrange
    Participant adminCaller = createParticipant("conv_1", 1L, Role.ADMIN);
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(adminCaller));
    when(participantRepository.findByConversationIdAndUserId("conv_1", 99L))
        .thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(
            withUser(conversationService.updateParticipantRole("conv_1", 99L, Role.ADMIN)))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.PARTICIPANT_NOT_FOUND)
        .verify();
  }

  // ==================== DELETE ====================

  @Test
  void deleteConversation_WhenNotAdmin_ReturnsError() {
    // Arrange
    Participant memberParticipant = createParticipant("conv_1", 1L, Role.MEMBER);
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(memberParticipant));

    // Act & Assert
    StepVerifier.create(withUser(conversationService.deleteConversation("conv_1")))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode()
                        == ErrorCode.CONVERSATION_UPDATE_UNAUTHORIZED)
        .verify();
  }

  @Test
  void deleteConversation_WhenAdmin_DeletesSuccessfully() {
    // Arrange
    Participant adminParticipant = createParticipant("conv_1", 1L, Role.ADMIN);
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(adminParticipant));

    Conversation conversation = new Conversation();
    conversation.setId("conv_1");
    conversation.setAvatarUrl(null);
    when(conversationRepository.findById("conv_1")).thenReturn(Mono.just(conversation));
    when(participantRepository.deleteByConversationId("conv_1")).thenReturn(Mono.empty());
    when(messageRepository.deleteByConversationId("conv_1")).thenReturn(Mono.empty());
    when(conversationRepository.delete(any(Conversation.class))).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(withUser(conversationService.deleteConversation("conv_1")))
        .assertNext(response -> assertNotNull(response))
        .verifyComplete();

    verify(conversationRepository).delete(any(Conversation.class));
    verify(participantRepository).deleteByConversationId("conv_1");
    verify(messageRepository).deleteByConversationId("conv_1");
  }

  @Test
  void deleteConversation_WithAvatar_DeletesAvatarToo() {
    // Arrange
    Participant adminParticipant = createParticipant("conv_1", 1L, Role.ADMIN);
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(adminParticipant));

    Conversation conversation = new Conversation();
    conversation.setId("conv_1");
    conversation.setAvatarUrl("https://firebase.com/avatar.png");
    when(conversationRepository.findById("conv_1")).thenReturn(Mono.just(conversation));
    when(firebaseStorageService.deleteFile("https://firebase.com/avatar.png"))
        .thenReturn(Mono.just(true));
    when(participantRepository.deleteByConversationId("conv_1")).thenReturn(Mono.empty());
    when(messageRepository.deleteByConversationId("conv_1")).thenReturn(Mono.empty());
    when(conversationRepository.delete(any(Conversation.class))).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(withUser(conversationService.deleteConversation("conv_1")))
        .assertNext(response -> assertNotNull(response))
        .verifyComplete();

    verify(firebaseStorageService).deleteFile("https://firebase.com/avatar.png");
  }

  @Test
  void deleteConversation_WhenConversationNotFound_ReturnsError() {
    // Arrange
    Participant adminParticipant = createParticipant("conv_1", 1L, Role.ADMIN);
    when(participantRepository.findByConversationIdAndUserId("conv_1", 1L))
        .thenReturn(Mono.just(adminParticipant));
    when(conversationRepository.findById("conv_1")).thenReturn(Mono.empty());

    // Act & Assert
    StepVerifier.create(withUser(conversationService.deleteConversation("conv_1")))
        .expectErrorMatches(
            e ->
                e instanceof AppException
                    && ((AppException) e).getErrorCode() == ErrorCode.CONVERSATION_NOT_FOUND)
        .verify();
  }

  // ==================== LOCATION ====================

  @Test
  void getConversationLiveLocations_ReturnsLocations() {
    // Arrange
    when(liveLocationService.getActiveLocations("conv_1", 1L)).thenReturn(Mono.just(List.of()));

    // Act & Assert
    StepVerifier.create(withUser(conversationService.getConversationLiveLocations("conv_1")))
        .assertNext(
            response -> {
              assertNotNull(response.getData());
              assertTrue(response.getData().isEmpty());
            })
        .verifyComplete();
  }

  // ==================== HELPERS ====================

  private Participant createParticipant(String conversationId, Long userId, Role role) {
    Participant participant = new Participant();
    participant.setId("p_" + userId);
    participant.setConversationId(conversationId);
    participant.setUserId(userId);
    participant.setRole(role);
    participant.setLastReadMessageSequence(0L);
    return participant;
  }
}
