package com.bitetogether.chat_service.dto.vote;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class CreateVoteSessionRequest {
  @NotNull private String conversationId;

  @NotEmpty private List<VoteOptionRequest> options;

  @Data
  public static class VoteOptionRequest {
    @NotBlank private String placeId;

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 300)
    private String address;
  }
}
