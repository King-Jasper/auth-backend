package com.mintfintech.savingsms.usecase.features.investment;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.InvestmentCreationRequest;
import com.mintfintech.savingsms.usecase.models.InvestmentCreationResponseModel;

public interface CreateInvestmentUseCase {

    InvestmentCreationResponseModel createInvestment(AuthenticatedUser authenticatedUser, InvestmentCreationRequest request);
}
