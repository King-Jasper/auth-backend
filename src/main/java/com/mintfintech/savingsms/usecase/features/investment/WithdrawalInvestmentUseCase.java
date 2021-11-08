package com.mintfintech.savingsms.usecase.features.investment;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.CorporateApprovalRequest;
import com.mintfintech.savingsms.usecase.data.request.InvestmentWithdrawalRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentLiquidationResponse;

/**
 * Created by jnwanya on
 * Thu, 20 May, 2021
 */
public interface WithdrawalInvestmentUseCase {
    InvestmentLiquidationResponse liquidateInvestment(AuthenticatedUser authenticatedUser, InvestmentWithdrawalRequest request);

    void processInterestPayout();

    void processPenaltyChargePayout();

    void processWithholdingTaxPayout();

    void processPrincipalPayout();
    String approveInvestmentWithdrawal(CorporateApprovalRequest request, AppUserEntity user, MintAccountEntity corporateAccount);
}
