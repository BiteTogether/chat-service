package com.bitetogether.chat_service.dto.vote;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CastVoteRequest {
  @NotBlank private String optionId;
}
