package com.mintfintech.savingsms.usecase.features.referral_savings;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.ReferralDetailsResponse;

public interface GetReferralRewardUseCase {
    ReferralDetailsResponse getReferralDetails(AuthenticatedUser authenticatedUser);
}
