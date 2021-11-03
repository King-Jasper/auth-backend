package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
public class CorporateInvestmentEvent {

    private String requestId;

    private String debitAccountId;

    private String userId;

    private BigDecimal totalAmount;

    private String transactionCategory;

    private String transactionType;

    private String approvalStatus;

    private LocalDateTime dateReviewed;

    private String statusUpdateReason;

    private String transactionDescription;

    private String mintAccountId;
}
