package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Tue, 31 Mar, 2020
 */
@Data
@Builder
public class SavingsPlanTenorModel {
    private long tenorId;
    private String description;
}
