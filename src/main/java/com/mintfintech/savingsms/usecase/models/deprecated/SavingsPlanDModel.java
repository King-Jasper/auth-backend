package com.mintfintech.savingsms.usecase.models.deprecated;

import com.mintfintech.savingsms.usecase.models.SavingsPlanTenorModel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by jnwanya on
 * Sun, 05 Jul, 2020
 */
@Data
@Builder
public class SavingsPlanDModel {
    private String planId;
    private String name;
    private BigDecimal minimumBalance;
    private BigDecimal maximumBalance;
    private double interestRate;
    private List<SavingsPlanTenorModel> durations;
}
