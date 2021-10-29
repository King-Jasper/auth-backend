package com.mintfintech.savingsms.usecase.features.corporate;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.CorporateTransactionSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.CorporateTransactionDetailResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.models.CorporateTransactionRequestModel;

public interface GetCorporateTransactionUseCase {
    CorporateTransactionDetailResponse getTransactionRequestDetail(AuthenticatedUser currentUser, String requestId);
    PagedDataResponse<CorporateTransactionRequestModel> getTransactionRequest(AuthenticatedUser currentUser, CorporateTransactionSearchRequest searchRequest, int page, int size);
}
