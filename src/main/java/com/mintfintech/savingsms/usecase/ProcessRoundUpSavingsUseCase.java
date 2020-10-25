package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.usecase.data.events.incoming.MintTransactionPayload;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
public interface ProcessRoundUpSavingsUseCase {
    void processTransactionForRoundUpSavings(MintTransactionPayload transactionPayload);
}
