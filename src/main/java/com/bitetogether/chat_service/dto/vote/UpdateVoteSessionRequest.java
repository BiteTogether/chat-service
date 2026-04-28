package com.bitetogether.chat_service.dto.vote;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateVoteSessionRequest {
  @NotBlank
  @Size(max = 120)
  private String name;
}
