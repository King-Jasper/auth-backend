package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CorporateInvestmentCreationEmailEvent {

    private BigDecimal amount;

    private int duration;

    private double interestRate;

    private String maturityDate;

    private String initiator;

    private String initiatorEmail;

    private String reviewer;

    private String reviewerEmail;

    private String approvalStatus;

    private LocalDateTime dateReviewed;

    private String statusUpdateReason;

    private String requestId;

    private String transactionType;
}
