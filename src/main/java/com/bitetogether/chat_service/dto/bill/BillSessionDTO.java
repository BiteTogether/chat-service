package com.bitetogether.chat_service.dto.bill;

import com.bitetogether.chat_service.enums.bill.BillShareStatus;
import com.bitetogether.chat_service.enums.bill.BillSplitType;
import com.bitetogether.chat_service.enums.bill.BillStatus;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

@Builder
public record BillSessionDTO(
    String id,
    String conversationId,
    String voteSessionId,
    Long createdBy,
    String currency,
    BigDecimal totalAmount,
    BillStatus status,
    BillSplitType splitType,
    List<BillShareDTO> shares) {
  @Builder
  public record BillShareDTO(
      Long userId,
      BigDecimal amount,
      BigDecimal paidAmount,
      Boolean paid,
      BillShareStatus status) {}
}
