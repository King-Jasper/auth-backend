package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Created by jnwanya on
 * Sun, 01 Nov, 2020
 */
@Builder
@Data
public class EmergencySavingsCreationRequest {
    private double fundingAmount;
    private String name;
    private double targetAmount;
    private LocalDate startDate;
    private String frequency;
    private boolean autoDebit;
}
