package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by jnwanya on
 * Tue, 31 Mar, 2020
 */
@Data
@Builder
public class SavingsPlanModel {
    private String planId;
    private String name;
    private BigDecimal minimumBalance;
    private BigDecimal maximumBalance;
    private double percentageInterestRate;
    private List<SavingsPlanTenorModel> savingsDurationList;
}
