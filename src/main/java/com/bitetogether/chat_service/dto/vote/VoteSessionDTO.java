package com.bitetogether.chat_service.dto.vote;

import com.bitetogether.chat_service.enums.VoteSessionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;

@Builder
public record VoteSessionDTO(
    String id,
    String conversationId,
    Long createdBy,
    VoteSessionStatus status,
    List<VoteOptionDTO> options,
    Map<Long, String> votes,
    String winnerOptionId,
    Instant closedAt) {
  @Builder
  public record VoteOptionDTO(
      String id,
      String placeId,
      String name,
      String address,
      Double lat,
      Double lng,
      String label) {}
}
