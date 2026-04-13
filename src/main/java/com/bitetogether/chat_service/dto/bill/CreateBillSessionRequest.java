package com.bitetogether.chat_service.dto.bill;

import com.bitetogether.chat_service.enums.BillSplitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class CreateBillSessionRequest {
  @NotNull private String conversationId;

  private String voteSessionId;

  @NotBlank private String currency;

  @NotNull
  @DecimalMin(value = "0.01")
  private BigDecimal totalAmount;

  @NotNull private BillSplitType splitType;

  private List<CustomSplitItem> customSplits;

  @Data
  public static class CustomSplitItem {
    @NotNull private Long userId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;
  }
}
