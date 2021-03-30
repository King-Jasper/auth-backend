package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Tue, 11 Aug, 2020
 */
@Data
@Builder
public class CustomerReferralEvent {
    private String accountId;
    private String userId;
    private String referredByUserId;
    private String referralCodeUsed;
    private String registrationPlatform;
}
