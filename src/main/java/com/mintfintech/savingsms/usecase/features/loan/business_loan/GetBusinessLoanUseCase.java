package com.mintfintech.savingsms.usecase.features.loan.business_loan;

import com.mintfintech.savingsms.domain.entities.HNILoanCustomerEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.BusinessLoanResponse;
import com.mintfintech.savingsms.usecase.data.response.HairFinanceLoanResponse;
import com.mintfintech.savingsms.usecase.data.response.LoanRequestScheduleResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Sat, 26 Feb, 2022
 */
public interface GetBusinessLoanUseCase {
    PagedDataResponse<BusinessLoanResponse> getBusinessLoanDetails(AuthenticatedUser currentUser, int page, int size);
    BusinessLoanResponse fromEntityToResponse(LoanRequestEntity loanRequest);
    HairFinanceLoanResponse fromEntityToHairFinanceResponse(LoanRequestEntity loanRequest);
    HairFinanceLoanResponse getHairFinanceLoanDetail(AuthenticatedUser currentUser, String loanId);
    LoanRequestScheduleResponse getRepaymentSchedule(AuthenticatedUser authenticatedUser, BigDecimal amount, int duration);
    LoanRequestScheduleResponse getRepaymentSchedule(HNILoanCustomerEntity hniLoanCustomer, BigDecimal amount, int duration);
}
