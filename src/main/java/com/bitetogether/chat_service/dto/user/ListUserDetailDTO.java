package com.bitetogether.chat_service.dto.user;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListUserDetailDTO {
  private List<UserDetailDTO> users;
}
