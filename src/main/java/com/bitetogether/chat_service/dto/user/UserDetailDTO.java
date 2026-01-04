package com.bitetogether.chat_service.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDetailDTO {
  private Long id;
  private String username;
  private String email;
  private String fullName;
  private String phoneNumber;
  private String avatar;
  private String role;
}
