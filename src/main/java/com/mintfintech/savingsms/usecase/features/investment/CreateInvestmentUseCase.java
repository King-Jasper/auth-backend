package com.mintfintech.savingsms.usecase.features.investment;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.InvestmentCreationRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentCreationResponse;

public interface CreateInvestmentUseCase {

    InvestmentCreationResponse createInvestment(AuthenticatedUser authenticatedUser, InvestmentCreationRequest request);
}