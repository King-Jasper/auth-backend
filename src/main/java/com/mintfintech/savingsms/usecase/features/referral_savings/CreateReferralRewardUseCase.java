package com.mintfintech.savingsms.usecase.features.referral_savings;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.usecase.data.events.incoming.CustomerReferralEvent;

import java.time.LocalDateTime;

/**
 * Created by jnwanya on
 * Tue, 15 Dec, 2020
 */
public interface CreateReferralRewardUseCase {
    void processCustomerReferralReward(CustomerReferralEvent referralEvent);
    void processReferredCustomerReward(MintAccountEntity mintAccountEntity, SavingsGoalEntity fundedSavingsGoal);
    String processReferralByUser(String userId, String phoneNumber);
    String processReferralByUserOld(String userId, String phoneNumber,  int size, boolean overrideTime);
    void processReferralBackLog(LocalDateTime start, LocalDateTime end, int size);
}
