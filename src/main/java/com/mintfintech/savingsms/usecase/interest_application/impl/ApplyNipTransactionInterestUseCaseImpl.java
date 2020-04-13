package com.mintfintech.savingsms.usecase.interest_application.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalTransactionEntity;
import com.mintfintech.savingsms.usecase.data.events.incoming.NipTransactionInterestEvent;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.interest_application.ApplyNipTransactionInterestUseCase;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Mon, 13 Apr, 2020
 */
@Named
@AllArgsConstructor
public class ApplyNipTransactionInterestUseCaseImpl implements ApplyNipTransactionInterestUseCase {

    private MintAccountEntityDao mintAccountEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private SavingsGoalTransactionEntity savingsGoalTransactionEntity;
    private AppUserEntityDao appUserEntityDao;

    @Override
    public void processNipInterest(NipTransactionInterestEvent nipTransactionInterestEvent) {
        MintAccountEntity mintAccountEntity = mintAccountEntityDao.findAccountByAccountId(nipTransactionInterestEvent.getAccountId())
                .orElseThrow(() -> new BusinessLogicConflictException("Account Id not found: "+nipTransactionInterestEvent.getAccountId()));
        AppUserEntity appUserEntity = appUserEntityDao.getAppUserByUserId(nipTransactionInterestEvent.getUserId());
       // savingsGoalEntityDao.

    }
}
