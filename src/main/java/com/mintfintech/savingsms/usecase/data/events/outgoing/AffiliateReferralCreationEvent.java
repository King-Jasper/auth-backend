package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AffiliateReferralCreationEvent {

    private String transactionDate;

    private String customerName;

    private BigDecimal transactionAmount;

    private String transactionType;

    private String referralCode;

    private int investmentTenorDuration;
}
