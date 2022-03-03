package com.mintfintech.savingsms.usecase.features.referral_savings.impl;

import com.mintfintech.savingsms.domain.dao.ReactHQReferralEntityDao;
import com.mintfintech.savingsms.usecase.data.events.incoming.AccountCreditEvent;
import com.mintfintech.savingsms.usecase.features.referral_savings.ReachHQTransactionUseCase;
import lombok.AllArgsConstructor;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Thu, 03 Mar, 2022
 */
@Named
@AllArgsConstructor
public class ReachHQTransactionUseCaseImpl implements ReachHQTransactionUseCase {
    private final ReactHQReferralEntityDao reactHQReferralEntityDao;

    @Override
    public void processCustomerDebit(AccountCreditEvent accountCreditEvent) {
        String accountNumber = accountCreditEvent.getAccountNumber();
        reactHQReferralEntityDao.findCustomerForDebit(accountNumber);
    }
}
