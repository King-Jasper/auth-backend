package com.mintfintech.savingsms.usecase.features;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.events.incoming.OnlinePaymentStatusUpdateEvent;
import com.mintfintech.savingsms.usecase.data.request.OnlineFundingRequest;
import com.mintfintech.savingsms.usecase.data.response.OnlineFundingResponse;
import com.mintfintech.savingsms.usecase.data.response.ReferenceGenerationResponse;

/**
 * Created by jnwanya on
 * Thu, 22 Oct, 2020
 */
public interface OnlineFundingUseCase {
    ReferenceGenerationResponse createFundingRequest(AuthenticatedUser authenticatedUser, OnlineFundingRequest fundingRequest);
    OnlineFundingResponse verifyFundingRequest(AuthenticatedUser authenticatedUser, String reference);
    void updateFundingPaymentStatus(OnlinePaymentStatusUpdateEvent paymentStatusUpdateEvent);
}
