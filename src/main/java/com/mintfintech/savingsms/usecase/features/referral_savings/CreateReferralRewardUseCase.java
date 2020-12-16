package com.mintfintech.savingsms.usecase.features.referral_savings;

import com.mintfintech.savingsms.usecase.data.events.incoming.CustomerReferralEvent;

/**
 * Created by jnwanya on
 * Tue, 15 Dec, 2020
 */
public interface CreateReferralRewardUseCase {
    void processCustomerReferralReward(CustomerReferralEvent referralEvent);
}
