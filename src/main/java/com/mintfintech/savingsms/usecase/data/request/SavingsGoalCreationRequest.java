package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

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
    // To be removed
    private long durationId;

    private int durationInDays;

    private boolean lockedSavings;

    private LocalDate startDate;

    private String frequency;

    private boolean autoDebit;
}
