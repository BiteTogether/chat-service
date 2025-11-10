package com.bitetogether.chat_service.mapper;

import com.bitetogether.chat_service.dto.request.ReactionRequest;
import com.bitetogether.chat_service.dto.response.ReactionResponse;
import com.bitetogether.chat_service.model.Reaction;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReactionMapper {
  Reaction toReaction(ReactionRequest reactionRequest);

  ReactionResponse toReactionResponse(Reaction reaction);

  @BeanMapping(
      nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
  void updateReactionFromReactionRequest(
      ReactionRequest reactionRequest, @MappingTarget Reaction reaction);
}
