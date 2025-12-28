package com.bitetogether.chat_service.mapper;

import com.bitetogether.chat_service.dto.SenderInfo;
import com.bitetogether.chat_service.dto.UserDTO;
import org.springframework.stereotype.Component;

@Component
public class SenderInfoMapper {
    public SenderInfo senderInfoFromUserDto(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }

        SenderInfo senderInfo = new SenderInfo();
        senderInfo.setId(userDTO.getId());
        senderInfo.setUsername(userDTO.getUsername());
        senderInfo.setFullName(userDTO.getFullName());
        senderInfo.setAvatar(userDTO.getAvatar());
        return senderInfo;
    }
}
