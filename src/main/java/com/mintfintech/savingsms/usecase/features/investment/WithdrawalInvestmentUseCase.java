package com.mintfintech.savingsms.usecase.features.investment;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.InvestmentWithdrawalRequest;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;

/**
 * Created by jnwanya on
 * Thu, 20 May, 2021
 */
public interface WithdrawalInvestmentUseCase {
    InvestmentModel liquidateInvestment(AuthenticatedUser authenticatedUser, InvestmentWithdrawalRequest request);

    void processInterestPayout();

    void processPenaltyChargePayout();

    void processWithholdingTaxPayout();

    void processPrincipalPayout();
}
