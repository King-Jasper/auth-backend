package com.mintfintech.savingsms.usecase.features.investment;

import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.CorporateApprovalRequest;
import com.mintfintech.savingsms.usecase.data.request.InvestmentFundingRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentFundingResponse;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Wed, 19 May, 2021
 */
public interface FundInvestmentUseCase {
    InvestmentTransactionEntity fundInvestment(InvestmentEntity investmentEntity, MintBankAccountEntity debitAccount, BigDecimal amount);
    InvestmentFundingResponse fundInvestment(AuthenticatedUser authenticatedUser, InvestmentFundingRequest request);
    InvestmentFundingResponse fundInvestmentByAdmin(InvestmentFundingRequest request);
    String approveCorporateInvestmentTopUp(CorporateApprovalRequest request, AppUserEntity user, MintAccountEntity corporateAccount);
}
