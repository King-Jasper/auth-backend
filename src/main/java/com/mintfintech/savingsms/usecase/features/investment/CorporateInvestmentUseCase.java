package com.mintfintech.savingsms.usecase.features.investment;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.CorporateApprovalRequest;
import com.mintfintech.savingsms.usecase.data.request.InvestmentCreationRequest;
import com.mintfintech.savingsms.usecase.data.response.CorporateInvestmentCreationResponse;
import com.mintfintech.savingsms.usecase.data.response.InvestmentCreationResponse;

public interface CorporateInvestmentUseCase {

    CorporateInvestmentCreationResponse createInvestment(AuthenticatedUser authenticatedUser, InvestmentCreationRequest request);

    String processApproval(AuthenticatedUser authenticatedUser, CorporateApprovalRequest request);
}
