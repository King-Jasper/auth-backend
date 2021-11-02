package com.mintfintech.savingsms.usecase.features.corporate;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.CorporateInvestmentDetailResponse;
import com.mintfintech.savingsms.usecase.data.response.CorporateInvestmentLiquidationDetailResponse;
import com.mintfintech.savingsms.usecase.data.response.CorporateInvestmentTopUpDetailResponse;

public interface GetCorporateTransactionUseCase {
    CorporateInvestmentDetailResponse getInvestmentRequestDetail(AuthenticatedUser currentUser, String requestId);
    CorporateInvestmentTopUpDetailResponse getInvestmentTopUpRequestDetail(AuthenticatedUser currentUser, String requestId);
    CorporateInvestmentLiquidationDetailResponse getInvestmentLiquidationRequestDetail(AuthenticatedUser currentUser, String requestId);
}
