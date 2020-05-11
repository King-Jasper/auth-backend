package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.usecase.data.events.incoming.AccountLimitUpdateEvent;
import com.mintfintech.savingsms.usecase.data.events.incoming.BankAccountTierUpgradeEvent;
import com.mintfintech.savingsms.usecase.data.events.incoming.MintAccountCreationEvent;
import com.mintfintech.savingsms.usecase.data.events.incoming.MintBankAccountCreationEvent;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
public interface AccountSetupUseCases {
    void createMintAccount(MintAccountCreationEvent mintAccountCreationEvent);
    void createIndividualBankAccount(MintBankAccountCreationEvent accountCreationEvent);
  //  void updateAccountTransactionLimit(AccountLimitUpdateEvent accountLimitUpdateEvent);
    void updateBankAccountTierLevel(BankAccountTierUpgradeEvent accountTierUpgradeEvent);
}
