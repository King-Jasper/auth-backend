package com.mintfintech.savingsms.usecase.features.investment;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CorporateTransactionRequestEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.CorporateApprovalRequest;
import com.mintfintech.savingsms.usecase.data.request.InvestmentCreationRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentCreationResponse;

public interface CreateInvestmentUseCase {

    InvestmentCreationResponse createInvestment(AuthenticatedUser authenticatedUser, InvestmentCreationRequest request);
    InvestmentCreationResponse createInvestmentByAdmin(AuthenticatedUser authenticatedUser, InvestmentCreationRequest request);
    String approveCorporateInvestment(CorporateTransactionRequestEntity requestEntity, CorporateApprovalRequest request, AppUserEntity user, MintAccountEntity corporateAccount);
}
