package com.mintfintech.savingsms.usecase.data.events.incoming;

import lombok.Data;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 13 Apr, 2020
 */
@Data
public class NipTransactionInterestEvent {
    private String debitAccountId;
    private String userId;
    private String internalReference;
    private String externalReference;
    private BigDecimal amount;
    private String dateCreated;
}
