package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Tue, 11 Feb, 2020
 */
@Builder
@Data
public class BankAccountTierUpgradeEvent {
    private String accountId;
    private String accountNumber;
    private String newTierLevel;
}
