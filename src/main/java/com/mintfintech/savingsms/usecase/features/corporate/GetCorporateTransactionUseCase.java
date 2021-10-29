package com.mintfintech.savingsms.usecase.features.corporate;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.CorporateTransactionDetailResponse;

public interface GetCorporateTransactionUseCase {
    CorporateTransactionDetailResponse getTransactionRequestDetail(AuthenticatedUser currentUser, String requestId);
}
