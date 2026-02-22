package com.bitetogether.chat_service.mapper;

import com.bitetogether.chat_service.dto.conversation.ConversationDTO;
import com.bitetogether.chat_service.dto.conversation.ParticipantDTO;
import com.bitetogether.chat_service.dto.message.ChatMessageDTO;
import com.bitetogether.chat_service.model.Conversation;
import com.bitetogether.chat_service.model.Participant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ConversationMapper {

  @Mapping(target = "participants", ignore = true)
  @Mapping(target = "unreadCount", ignore = true)
  @Mapping(target = "latestMessage", ignore = true)
  ConversationDTO toDTO(Conversation conversation);

  default ConversationDTO toDTO(
      Conversation conversation,
      List<ParticipantDTO> participants,
      Long unreadCount,
      ChatMessageDTO latestMessage) {
    ConversationDTO dto = toDTO(conversation);
    dto.setParticipants(participants);
    dto.setUnreadCount(unreadCount);
    dto.setLatestMessage(latestMessage);
    return dto;
  }

  @Mapping(target = "username", ignore = true)
  @Mapping(target = "avatarUrl", ignore = true)
  ParticipantDTO toParticipantDTO(Participant participant);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "type", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "updatedBy", ignore = true)
  void updateConversationFromRequest(
      @MappingTarget Conversation conversation,
      com.bitetogether.chat_service.dto.conversation.UpdateConversationRequest request);
}

