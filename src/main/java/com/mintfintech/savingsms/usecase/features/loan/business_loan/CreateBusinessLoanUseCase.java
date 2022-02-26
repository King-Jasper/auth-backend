package com.mintfintech.savingsms.usecase.features.loan.business_loan;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.BusinessLoanResponse;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Fri, 25 Feb, 2022
 */
public interface CreateBusinessLoanUseCase {
    BusinessLoanResponse createRequest(AuthenticatedUser authenticatedUser, BigDecimal loanAmount, int durationInMonths, String creditAccountId);
}
