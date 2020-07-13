package com.mintfintech.savingsms.usecase.backoffice;

import com.mintfintech.savingsms.usecase.data.response.SavingsMaturityStatSummary;

import java.time.LocalDate;

/**
 * Created by jnwanya on
 * Mon, 22 Jun, 2020
 */
public interface GetSavingsTransactionUseCase {
    SavingsMaturityStatSummary getSavingsMaturityStatistics(LocalDate fromDate, LocalDate toDate);
}
