package com.bitetogether.chat_service.mapper;

import com.bitetogether.chat_service.dto.message.ChatMessageDTO;
import com.bitetogether.chat_service.model.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

  @Mapping(target = "content", ignore = true)
  @Mapping(target = "seq", source = "sequence")
  @Mapping(target = "createdAt", source = "createdAt")
  @Mapping(target = "updatedAt", source = "updatedAt")
  @Mapping(target = "createdBy", source = "createdBy")
  @Mapping(target = "updatedBy", source = "updatedBy")
  ChatMessageDTO toChatMessageDTO(Message message);

  default ChatMessageDTO toChatMessageDTO(Message message, String decryptedContent) {
    ChatMessageDTO dto = toChatMessageDTO(message);
    dto.setContent(decryptedContent);
    return dto;
  }
}
