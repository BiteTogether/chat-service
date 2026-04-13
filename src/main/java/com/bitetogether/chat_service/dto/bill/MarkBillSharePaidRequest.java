package com.bitetogether.chat_service.dto.bill;

import lombok.Data;

@Data
public class MarkBillSharePaidRequest {
  // Optional: creator can confirm payment for another participant
  private Long userId;
}
