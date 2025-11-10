package com.bitetogether.chat_service.mapper;

import com.bitetogether.chat_service.dto.request.MessageRequest;
import com.bitetogether.chat_service.dto.response.MessageResponse;
import com.bitetogether.chat_service.model.Message;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface MessageMapper {
  Message toMessage(MessageRequest messageRequest);

  MessageResponse toMessageResponse(Message message);

  @BeanMapping(
      nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
  void updateMessageFromMessageRequest(
      MessageRequest messageRequest, @MappingTarget Message message);
}
