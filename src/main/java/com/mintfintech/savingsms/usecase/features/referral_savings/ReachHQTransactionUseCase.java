package com.mintfintech.savingsms.usecase.features.referral_savings;

import com.mintfintech.savingsms.usecase.data.events.incoming.AccountCreditEvent;
/**
 * Created by jnwanya on
 * Thu, 03 Mar, 2022
 */
public interface ReachHQTransactionUseCase {
    void processCustomerDebit(AccountCreditEvent accountCreditEvent);
    boolean processCustomerDebit(String accountNumber, boolean createRecord, boolean canBeCredited);
    void processCustomerCredit();
}
