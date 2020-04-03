package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Data
@Builder
public class SavingsGoalCreationRequest {
    private String planId;
    private String categoryCode;
    private double fundingAmount;
    private String debitAccountId;
    private String name;
    private double targetAmount;
    private long durationId;
}
