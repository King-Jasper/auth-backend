package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Tue, 16 Mar, 2021
 */
@Data
@Builder
public class BankAccountStatusUpdateEvent  {
    private String accountId;
    private String accountNumber;
    private String status;
}
