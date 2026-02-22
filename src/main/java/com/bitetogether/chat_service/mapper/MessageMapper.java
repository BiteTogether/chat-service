package com.bitetogether.chat_service.mapper;

import com.bitetogether.chat_service.dto.message.ChatMessageDTO;
import com.bitetogether.chat_service.model.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MessageMapper {

  @Mapping(target = "content", ignore = true)
  @Mapping(target = "seq", source = "sequence")
  ChatMessageDTO toChatMessageDTO(Message message);

  default ChatMessageDTO toChatMessageDTO(Message message, String decryptedContent) {
    ChatMessageDTO dto = toChatMessageDTO(message);
    dto.setContent(decryptedContent);
    return dto;
  }
}
