package com.mintfintech.savingsms.usecase.features.roundup_savings;

import com.mintfintech.savingsms.usecase.data.events.incoming.MintTransactionPayload;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
public interface ProcessRoundUpSavingsUseCase {
    void processTransactionForRoundUpSavings(MintTransactionPayload transactionPayload);

    void processTransactionForSpendAndSave(MintTransactionPayload transactionPayload);
}
