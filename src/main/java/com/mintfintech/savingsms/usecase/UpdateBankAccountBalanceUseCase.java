package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.usecase.data.events.incoming.AccountBalanceUpdateEvent;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
public interface UpdateBankAccountBalanceUseCase {
    void processBalanceUpdate(AccountBalanceUpdateEvent balanceUpdateEvent);
    MintBankAccountEntity processBalanceUpdate(MintBankAccountEntity bankAccountEntity);
}
