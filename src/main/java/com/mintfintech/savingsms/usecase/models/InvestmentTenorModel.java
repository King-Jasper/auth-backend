package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvestmentTenorModel {
    private long durationId;
    private String description;
    private double interestRate;
    private double penaltyRate;
    private int minimumDuration;
    private int maximumDuration;
}
