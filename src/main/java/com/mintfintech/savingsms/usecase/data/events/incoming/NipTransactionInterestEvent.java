package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 13 Apr, 2020
 */
@Data
public class NipTransactionInterestEvent {
    private String accountId;
    private String userId;
    private String reference;
    private BigDecimal amount;
    private String dateCreated;
}
