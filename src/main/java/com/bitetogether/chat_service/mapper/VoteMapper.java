package com.bitetogether.chat_service.mapper;

import com.bitetogether.chat_service.dto.vote.VoteSessionDTO;
import com.bitetogether.chat_service.model.VoteSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VoteMapper {

  @Mapping(target = "createdBy", source = "creatorId")
  VoteSessionDTO toDto(VoteSession voteSession);

  VoteSessionDTO.VoteOptionDTO toOptionDto(VoteSession.VoteOption voteOption);
}
