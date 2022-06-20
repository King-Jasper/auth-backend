package com.mintfintech.savingsms.usecase.data.response;

import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReferralDetailsResponse {

    private BigDecimal totalEarnings;

    private int numberOfCustomersReferred;

    private long referrerAmount;

    private long referredAirtimeAmount;

    private String referralMessage;

    private BigDecimal availableBalance;

    private SavingsGoalModel referralPurse;
}
