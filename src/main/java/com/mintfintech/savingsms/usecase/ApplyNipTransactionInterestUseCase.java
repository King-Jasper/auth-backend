package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.usecase.data.events.incoming.NipTransactionInterestEvent;

/**
 * Created by jnwanya on
 * Mon, 13 Apr, 2020
 */
public interface ApplyNipTransactionInterestUseCase {
    void processNipInterest(NipTransactionInterestEvent nipTransactionInterestEvent);

}
