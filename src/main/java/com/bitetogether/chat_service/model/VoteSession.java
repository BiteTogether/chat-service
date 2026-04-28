package com.bitetogether.chat_service.model;

import com.bitetogether.chat_service.enums.vote.VoteSessionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "vote_sessions")
@Data
@EqualsAndHashCode(callSuper = true)
public class VoteSession extends BaseModel {
  @Id private String id;

  @Field("conversation_id")
  private String conversationId;

  @Field("creator_id")
  private Long creatorId;

  @Field("name")
  private String name;

  @Field("status")
  private VoteSessionStatus status;

  @Field("options")
  private List<VoteOption> options;

  @Field("votes")
  private Map<Long, String> votes;

  @Field("winner_option_id")
  private String winnerOptionId;

  @Field("closed_at")
  private Instant closedAt;

  @Data
  public static class VoteOption {
    @Field("id")
    private String id;

    @Field("label")
    private String label;

    @Field("place_id")
    private String placeId;

    @Field("name")
    private String name;

    @Field("address")
    private String address;
  }
}
