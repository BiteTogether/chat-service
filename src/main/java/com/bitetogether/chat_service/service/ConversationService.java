package com.bitetogether.chat_service.service;

import com.bitetogether.chat_service.dto.conversation.ConversationDTO;
import com.bitetogether.chat_service.dto.conversation.CreateConversationRequest;
import com.bitetogether.chat_service.dto.conversation.ParticipantDTO;
import com.bitetogether.chat_service.dto.conversation.UpdateConversationRequest;
import com.bitetogether.chat_service.dto.message.ChatMessageDTO;
import com.bitetogether.chat_service.enums.ConversationType;
import com.bitetogether.chat_service.enums.Role;
import com.bitetogether.chat_service.exception.ErrorCode;
import com.bitetogether.chat_service.mapper.ConversationMapper;
import com.bitetogether.chat_service.mapper.MessageMapper;
import com.bitetogether.chat_service.model.Conversation;
import com.bitetogether.chat_service.model.Participant;
import com.bitetogether.chat_service.repository.ConversationRepository;
import com.bitetogether.chat_service.repository.MessageRepository;
import com.bitetogether.chat_service.repository.ParticipantRepository;
import com.bitetogether.common.dto.ApiResponseDTO;
import com.bitetogether.common.enums.ApiResponseStatus;
import com.bitetogether.common.exception.AppException;
import com.bitetogether.common.util.ApiResponseUtil;
import com.bitetogether.common.util.ReactiveUserContextUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationService {

  ConversationRepository conversationRepository;
  ParticipantRepository participantRepository;
  MessageRepository messageRepository;
  ConversationMapper conversationMapper;
  MessageMapper messageMapper;
  CryptoService cryptoService;

  // ==================== CREATE ====================

  /**
   * Create a new conversation with participants.
   */
  @Transactional
  public Mono<ApiResponseDTO<ConversationDTO>> createConversation(CreateConversationRequest request) {
    return ReactiveUserContextUtils.getUserIdOrError("User ID not found in context")
        .flatMap(currentUserId -> {
          // Add current user to participants
          Set<Long> allParticipantIds = new HashSet<>(request.getParticipantIds());
          allParticipantIds.add(currentUserId);

          // Validate request based on conversation type
          return validateCreateRequest(request, allParticipantIds)
              .then(checkExistingDirectConversation(request, allParticipantIds))
              .switchIfEmpty(createNewConversation(request, allParticipantIds, currentUserId));
        });
  }

  private Mono<Void> validateCreateRequest(CreateConversationRequest request, Set<Long> participantIds) {
    if (request.getType() == ConversationType.DIRECT) {
      if (participantIds.size() != 2) {
        return Mono.error(new AppException(ErrorCode.DIRECT_CONVERSATION_REQUIRES_TWO_PARTICIPANTS));
      }
    } else if (request.getType() == ConversationType.GROUP) {
      if (request.getName() == null || request.getName().isBlank()) {
        return Mono.error(new AppException(ErrorCode.GROUP_CONVERSATION_REQUIRES_NAME));
      }
    }
    return Mono.empty();
  }

  private Mono<ApiResponseDTO<ConversationDTO>> checkExistingDirectConversation(
      CreateConversationRequest request, Set<Long> participantIds) {
    if (request.getType() != ConversationType.DIRECT) {
      return Mono.empty();
    }

    List<Long> userIds = participantIds.stream().toList();
    Long user1 = userIds.get(0);
    Long user2 = userIds.get(1);

    // Find conversations where both users are participants
    return participantRepository.findByUserId(user1)
        .flatMap(p1 -> participantRepository.findByConversationIdAndUserId(p1.getConversationId(), user2)
            .map(p2 -> p1.getConversationId()))
        .collectList()
        .flatMap(conversationIds -> {
          if (conversationIds.isEmpty()) {
            return Mono.empty();
          }
          // Check if any of these are DIRECT conversations
          return conversationRepository.findByIdIn(new HashSet<>(conversationIds))
              .filter(c -> c.getType() == ConversationType.DIRECT)
              .next()
              .flatMap(existingConversation ->
                  getConversationById(existingConversation.getId())
              );
        });
  }

  private Mono<ApiResponseDTO<ConversationDTO>> createNewConversation(
      CreateConversationRequest request, Set<Long> participantIds, Long creatorId) {

    Conversation conversation = new Conversation();
    conversation.setType(request.getType());
    conversation.setName(request.getName());
    conversation.setAvatarUrl(request.getAvatarUrl());

    return conversationRepository.save(conversation)
        .flatMap(savedConversation ->
            createParticipants(savedConversation.getId(), participantIds, creatorId)
                .then(getConversationById(savedConversation.getId()))
        )
        .map(response -> ApiResponseUtil.buildApiResponse(
            ApiResponseStatus.CREATED,
            "Conversation created successfully",
            response.getData()));
  }

  private Flux<Participant> createParticipants(String conversationId, Set<Long> userIds, Long creatorId) {
    return Flux.fromIterable(userIds)
        .map(userId -> {
          Participant participant = new Participant();
          participant.setConversationId(conversationId);
          participant.setUserId(userId);
          // Creator becomes ADMIN, others become MEMBER
          participant.setRole(userId.equals(creatorId) ? Role.ADMIN : Role.MEMBER);
          participant.setLastReadMessageSequence(0L);
          return participant;
        })
        .flatMap(participantRepository::save);
  }

  // ==================== READ ====================

  /**
   * Get a conversation by ID.
   */
  public Mono<ApiResponseDTO<ConversationDTO>> getConversationById(String conversationId) {
    return ReactiveUserContextUtils.getUserIdOrError("User ID not found in context")
        .flatMap(currentUserId ->
            validateParticipant(conversationId, currentUserId)
                .then(buildConversationDTO(conversationId, currentUserId))
        )
        .map(dto -> ApiResponseUtil.buildApiResponse(
            ApiResponseStatus.SUCCESS,
            "Conversation retrieved successfully",
            dto));
  }

  /**
   * Get all conversations for the current user.
   */
  public Mono<ApiResponseDTO<List<ConversationDTO>>> getMyConversations() {
    return ReactiveUserContextUtils.getUserIdOrError("User ID not found in context")
        .flatMapMany(participantRepository::findByUserId)
        .flatMap(participant -> buildConversationDTO(participant.getConversationId(), participant.getUserId()))
        .collectList()
        .map(conversations -> ApiResponseUtil.buildApiResponse(
            ApiResponseStatus.SUCCESS,
            "Conversations retrieved successfully",
            conversations));
  }

  private Mono<ConversationDTO> buildConversationDTO(String conversationId, Long currentUserId) {
    return conversationRepository.findById(conversationId)
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.CONVERSATION_NOT_FOUND)))
        .flatMap(conversation -> {
          Mono<List<ParticipantDTO>> participantsMono = getParticipantDTOs(conversationId);
          Mono<Long> unreadCountMono = calculateUnreadCount(conversationId, currentUserId);
          Mono<ChatMessageDTO> latestMessageMono = getLatestMessage(conversationId);

          return Mono.zip(
              participantsMono,
              unreadCountMono,
              latestMessageMono.map(java.util.Optional::of).defaultIfEmpty(java.util.Optional.empty())
          ).map(tuple -> conversationMapper.toDTO(
              conversation,
              tuple.getT1(),
              tuple.getT2(),
              tuple.getT3().orElse(null)));
        });
  }

  private Mono<ChatMessageDTO> getLatestMessage(String conversationId) {
    return messageRepository.findTopByConversationIdOrderBySequenceDesc(conversationId)
        .flatMap(message -> cryptoService.decrypt(message.getCiphertext(), message.getIv(), message.getAuthTag())
            .map(decryptedContent -> messageMapper.toChatMessageDTO(message, decryptedContent)));
  }

  private Mono<List<ParticipantDTO>> getParticipantDTOs(String conversationId) {
    return participantRepository.findByConversationId(conversationId)
        .map(conversationMapper::toParticipantDTO)
        // TODO: Enrich with user info from UserClient
        .collectList();
  }

  private Mono<Long> calculateUnreadCount(String conversationId, Long userId) {
    return participantRepository.findByConversationIdAndUserId(conversationId, userId)
        .flatMap(participant -> {
          Long lastRead = participant.getLastReadMessageSequence();
          if (lastRead == null) lastRead = 0L;
          return messageRepository.countByConversationIdAndSequenceGreaterThan(conversationId, lastRead);
        })
        .defaultIfEmpty(0L);
  }

  // ==================== UPDATE ====================

  /**
   * Update a conversation (name, avatar).
   */
  @Transactional
  public Mono<ApiResponseDTO<ConversationDTO>> updateConversation(
      String conversationId, UpdateConversationRequest request) {
    return ReactiveUserContextUtils.getUserIdOrError("User ID not found in context")
        .flatMap(currentUserId ->
            validateAdminParticipant(conversationId, currentUserId)
                .then(conversationRepository.findById(conversationId))
        )
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.CONVERSATION_NOT_FOUND)))
        .flatMap(conversation -> {
          conversationMapper.updateConversationFromRequest(conversation, request);
          return conversationRepository.save(conversation);
        })
        .flatMap(savedConversation -> getConversationById(savedConversation.getId()))
        .map(response -> ApiResponseUtil.buildApiResponse(
            ApiResponseStatus.SUCCESS,
            "Conversation updated successfully",
            response.getData()));
  }

  /**
   * Add a participant to a conversation.
   */
  @Transactional
  public Mono<ApiResponseDTO<ParticipantDTO>> addParticipant(String conversationId, Long userId) {
    return ReactiveUserContextUtils.getUserIdOrError("User ID not found in context")
        .flatMap(currentUserId -> validateAdminParticipant(conversationId, currentUserId))
        .then(participantRepository.existsByConversationIdAndUserId(conversationId, userId))
        .flatMap(exists -> {
          if (exists) {
            return Mono.error(new AppException(ErrorCode.ALREADY_A_PARTICIPANT));
          }
          Participant participant = new Participant();
          participant.setConversationId(conversationId);
          participant.setUserId(userId);
          participant.setRole(Role.MEMBER);
          participant.setLastReadMessageSequence(0L);
          return participantRepository.save(participant);
        })
        .map(conversationMapper::toParticipantDTO)
        .map(dto -> ApiResponseUtil.buildApiResponse(
            ApiResponseStatus.CREATED,
            "Participant added successfully",
            dto));
  }

  /**
   * Remove a participant from a conversation.
   */
  @Transactional
  public Mono<ApiResponseDTO<Void>> removeParticipant(String conversationId, Long userId) {
    return ReactiveUserContextUtils.getUserIdOrError("User ID not found in context")
        .flatMap(currentUserId -> {
          // User can remove themselves, or admin can remove others
          if (currentUserId.equals(userId)) {
            return validateParticipant(conversationId, currentUserId);
          }
          return validateAdminParticipant(conversationId, currentUserId);
        })
        .then(checkCanRemoveParticipant(conversationId, userId))
        .then(participantRepository.deleteByConversationIdAndUserId(conversationId, userId))
        .then(Mono.fromCallable(() ->
            ApiResponseUtil.<Void>buildApiResponse(
                ApiResponseStatus.SUCCESS,
                "Participant removed successfully",
                null)));
  }

  private Mono<Void> checkCanRemoveParticipant(String conversationId, Long userId) {
    return participantRepository.findByConversationIdAndUserId(conversationId, userId)
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.PARTICIPANT_NOT_FOUND)))
        .flatMap(participant -> {
          if (participant.getRole() == Role.ADMIN) {
            // Check if this is the last admin
            return participantRepository.countByConversationIdAndRole(conversationId, Role.ADMIN)
                .flatMap(adminCount -> {
                  if (adminCount <= 1) {
                    return Mono.error(new AppException(ErrorCode.CANNOT_REMOVE_LAST_ADMIN));
                  }
                  return Mono.empty();
                });
          }
          return Mono.empty();
        });
  }

  /**
   * Update participant role.
   */
  @Transactional
  public Mono<ApiResponseDTO<ParticipantDTO>> updateParticipantRole(
      String conversationId, Long userId, Role newRole) {
    return ReactiveUserContextUtils.getUserIdOrError("User ID not found in context")
        .flatMap(currentUserId -> validateAdminParticipant(conversationId, currentUserId))
        .then(participantRepository.findByConversationIdAndUserId(conversationId, userId))
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.PARTICIPANT_NOT_FOUND)))
        .flatMap(participant -> {
          // Check if demoting last admin
          if (participant.getRole() == Role.ADMIN && newRole == Role.MEMBER) {
            return participantRepository.countByConversationIdAndRole(conversationId, Role.ADMIN)
                .flatMap(adminCount -> {
                  if (adminCount <= 1) {
                    return Mono.error(new AppException(ErrorCode.CANNOT_REMOVE_LAST_ADMIN));
                  }
                  participant.setRole(newRole);
                  return participantRepository.save(participant);
                });
          }
          participant.setRole(newRole);
          return participantRepository.save(participant);
        })
        .map(conversationMapper::toParticipantDTO)
        .map(dto -> ApiResponseUtil.buildApiResponse(
            ApiResponseStatus.SUCCESS,
            "Participant role updated successfully",
            dto));
  }

  /**
   * Mark messages as read up to a certain sequence.
   */
  @Transactional
  public Mono<ApiResponseDTO<Void>> markAsRead(String conversationId, Long lastReadSequence) {
    return ReactiveUserContextUtils.getUserIdOrError("User ID not found in context")
        .flatMap(currentUserId ->
            participantRepository.findByConversationIdAndUserId(conversationId, currentUserId))
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.NOT_A_PARTICIPANT)))
        .flatMap(participant -> {
          if (lastReadSequence > participant.getLastReadMessageSequence()) {
            participant.setLastReadMessageSequence(lastReadSequence);
            return participantRepository.save(participant);
          }
          return Mono.just(participant);
        })
        .then(Mono.fromCallable(() ->
            ApiResponseUtil.<Void>buildApiResponse(
                ApiResponseStatus.SUCCESS,
                "Messages marked as read",
                null)));
  }

  // ==================== DELETE ====================

  /**
   * Delete a conversation and all related data.
   */
  @Transactional
  public Mono<ApiResponseDTO<Void>> deleteConversation(String conversationId) {
    return ReactiveUserContextUtils.getUserIdOrError("User ID not found in context")
        .flatMap(currentUserId -> validateAdminParticipant(conversationId, currentUserId))
        .then(conversationRepository.findById(conversationId))
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.CONVERSATION_NOT_FOUND)))
        .flatMap(conversation ->
            // Delete all related data
            participantRepository.deleteByConversationId(conversationId)
                .then(messageRepository.deleteByConversationId(conversationId))
                .then(conversationRepository.delete(conversation))
        )
        .then(Mono.fromCallable(() ->
            ApiResponseUtil.<Void>buildApiResponse(
                ApiResponseStatus.SUCCESS,
                "Conversation deleted successfully",
                null)));
  }

  // ==================== VALIDATION HELPERS ====================

  private Mono<Void> validateParticipant(String conversationId, Long userId) {
    return participantRepository.existsByConversationIdAndUserId(conversationId, userId)
        .flatMap(exists ->
            exists ? Mono.empty() : Mono.error(new AppException(ErrorCode.NOT_A_PARTICIPANT)));
  }

  private Mono<Void> validateAdminParticipant(String conversationId, Long userId) {
    return participantRepository.findByConversationIdAndUserId(conversationId, userId)
        .switchIfEmpty(Mono.error(new AppException(ErrorCode.NOT_A_PARTICIPANT)))
        .flatMap(participant -> {
          if (participant.getRole() != Role.ADMIN) {
            return Mono.error(new AppException(ErrorCode.CONVERSATION_UPDATE_UNAUTHORIZED));
          }
          return Mono.empty();
        });
  }
}
