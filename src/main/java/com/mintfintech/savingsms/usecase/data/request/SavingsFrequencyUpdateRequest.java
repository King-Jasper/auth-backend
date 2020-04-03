package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Builder
@Data
public class SavingsFrequencyUpdateRequest {
    private String goalId;
    private String frequency;
    private double amount;
}
