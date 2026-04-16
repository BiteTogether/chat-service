package com.bitetogether.chat_service.mapper;

import com.bitetogether.chat_service.dto.bill.BillSessionDTO;
import com.bitetogether.chat_service.model.BillSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BillMapper {

  @Mapping(target = "createdBy", source = "creatorId")
  BillSessionDTO toDto(BillSession billSession);

  BillSessionDTO.BillShareDTO toShareDto(BillSession.BillShare billShare);
}
