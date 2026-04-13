package com.bitetogether.chat_service.model;

import com.bitetogether.chat_service.enums.BillShareStatus;
import com.bitetogether.chat_service.enums.BillSplitType;
import com.bitetogether.chat_service.enums.BillStatus;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "bill_sessions")
@Data
@EqualsAndHashCode(callSuper = true)
public class BillSession extends BaseModel {
  @Id private String id;

  @Field("conversation_id")
  private String conversationId;

  @Field("vote_session_id")
  private String voteSessionId;

  @Field("creator_id")
  private Long creatorId;

  @Field("currency")
  private String currency;

  @Field("total_amount")
  private BigDecimal totalAmount;

  @Field("status")
  private BillStatus status;

  @Field("split_type")
  private BillSplitType splitType;

  @Field("shares")
  private List<BillShare> shares;

  @Data
  public static class BillShare {
    @Field("user_id")
    private Long userId;

    @Field("amount")
    private BigDecimal amount;

    @Field("paid_amount")
    private BigDecimal paidAmount;

    @Field("paid")
    private Boolean paid;

    @Field("status")
    private BillShareStatus status;
  }
}
