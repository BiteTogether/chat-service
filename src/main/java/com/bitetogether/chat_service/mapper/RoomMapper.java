package com.bitetogether.chat_service.mapper;

import com.bitetogether.chat_service.dto.request.RoomDetailResponse;
import com.bitetogether.chat_service.dto.request.RoomRequest;
import com.bitetogether.chat_service.dto.request.RoomResponse;
import com.bitetogether.chat_service.model.Room;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoomMapper {
  Room toRoom(RoomRequest roomRequest);

  RoomResponse toRoomResponse(Room room);

  RoomDetailResponse toRoomDetailResponse(Room room);

  @BeanMapping(
      nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.IGNORE)
  void updateRoomFromRoomRequest(RoomRequest roomRequest, @MappingTarget Room room);
}
