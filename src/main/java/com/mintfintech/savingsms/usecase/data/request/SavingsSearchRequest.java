package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Created by jnwanya on
 * Sat, 06 Jun, 2020
 */
@Data
@Builder
public class SavingsSearchRequest {
    private String accountId;
    private String savingsType;
    private String goalId;
    private String savingsStatus;
    private String autoSavedStatus;
    private LocalDate fromDate;
    private LocalDate toDate;
}
