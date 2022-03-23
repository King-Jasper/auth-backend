package com.mintfintech.savingsms.usecase.backoffice;

import java.time.LocalDate;

import com.mintfintech.savingsms.usecase.data.request.InvestmentTransactionSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentTransactionSearchResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.data.response.SavingsMaturityStatSummary;

/**
 * Created by jnwanya on Mon, 22 Jun, 2020
 */
public interface GetSavingsTransactionUseCase {
	SavingsMaturityStatSummary getSavingsMaturityStatistics(LocalDate fromDate, LocalDate toDate);
	PagedDataResponse<InvestmentTransactionSearchResponse> getInvestmentTransactions(InvestmentTransactionSearchRequest request, int pageIndex, int size);
}
