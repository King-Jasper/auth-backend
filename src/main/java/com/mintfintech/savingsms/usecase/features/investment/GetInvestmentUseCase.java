package com.mintfintech.savingsms.usecase.features.investment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.InvestmentSearchRequest;
import com.mintfintech.savingsms.usecase.data.request.InvestmentTransactionSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentMaturityStatSummary;
import com.mintfintech.savingsms.usecase.data.response.InvestmentStatSummary;
import com.mintfintech.savingsms.usecase.data.response.InvestmentTransactionSearchResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import com.mintfintech.savingsms.usecase.models.InvestmentTransactionModel;

public interface GetInvestmentUseCase {
	List<InvestmentTransactionModel> getInvestmentTransactions(String investmentId);

	InvestmentModel toInvestmentModel(InvestmentEntity investment);

	InvestmentStatSummary getPagedInvestments(InvestmentSearchRequest searchRequest, int page, int size);

	InvestmentMaturityStatSummary getMaturityStatistics(LocalDate fromDate, LocalDate toDate);

	InvestmentStatSummary getPagedInvestmentsByAdmin(InvestmentSearchRequest searchRequest, int page, int size);

	BigDecimal calculateTotalExpectedReturn(BigDecimal amountInvested, BigDecimal currentAccruedInterest, double interestRate, LocalDateTime maturityTime);

	BigDecimal calculateOutstandingInterest(BigDecimal amountInvested, double interestRate, LocalDateTime maturityTime);

	PagedDataResponse<InvestmentTransactionSearchResponse> getInvestmentTransactions(InvestmentTransactionSearchRequest request, int pageIndex, int size);

	List<InvestmentTransactionModel> getUserInvestmentTransactions(AuthenticatedUser authenticatedUser, String debitAccountId);
}
