package com.mintfintech.savingsms.usecase.interest_application.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.usecase.data.events.incoming.NipTransactionInterestEvent;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.interest_application.ApplyNipTransactionInterestUseCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Mon, 13 Apr, 2020
 */
@Slf4j
@Named
@AllArgsConstructor
public class ApplyNipTransactionInterestUseCaseImpl implements ApplyNipTransactionInterestUseCase {

    private MintAccountEntityDao mintAccountEntityDao;
    private MintBankAccountEntityDao mintBankAccountEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    private AppUserEntityDao appUserEntityDao;
    private ApplicationProperty applicationProperty;

    @Override
    public void processNipInterest(NipTransactionInterestEvent nipTransactionInterestEvent) {
        if(nipTransactionInterestEvent.getAmount().compareTo(BigDecimal.valueOf(50000)) < 0) {
            log.info("Nip transaction {} is below nip interest amount: 50000", nipTransactionInterestEvent.getAmount());
            return;
        }
       /* MintAccountEntity mintAccountEntity = mintAccountEntityDao.findAccountByAccountId(nipTransactionInterestEvent.getAccountId())
                .orElseThrow(() -> new NotFoundException("Account Id not found: "+nipTransactionInterestEvent.getAccountId()));*/
        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.findByAccountId(nipTransactionInterestEvent.getDebitAccountId())
                .orElseThrow(() -> new NotFoundException("Bank account Id not found "+ nipTransactionInterestEvent.getDebitAccountId()));
        MintAccountEntity mintAccountEntity = debitAccount.getMintAccount();
        MintBankAccountEntity savingsAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(mintAccountEntity, BankAccountTypeConstant.SAVING);
        AppUserEntity appUserEntity = appUserEntityDao.getAppUserByUserId(nipTransactionInterestEvent.getUserId());
        Optional<SavingsGoalEntity> savingsGoalEntityOptional = savingsGoalEntityDao.findFirstSavingsByType(mintAccountEntity, SavingsGoalTypeConstant.MINT_DEFAULT_SAVINGS);
        if(!savingsGoalEntityOptional.isPresent()) {
            log.info("Default savings goal does not exist for account: {}", mintAccountEntity.getAccountId());
            return;
        }
        SavingsGoalEntity defaultGoal = savingsGoalEntityOptional.get();
        BigDecimal newBalance = defaultGoal.getSavingsBalance().add(BigDecimal.valueOf(applicationProperty.getNipTransactionInterest()));

        SavingsGoalTransactionEntity transactionEntity = SavingsGoalTransactionEntity.builder()
                .transactionType(TransactionTypeConstant.CREDIT)
                .transactionAmount(BigDecimal.valueOf(applicationProperty.getNipTransactionInterest()))
                .transactionStatus(TransactionStatusConstant.SUCCESSFUL)
                .transactionReference(nipTransactionInterestEvent.getInternalReference())
                .transactionResponseCode("00")
                .transactionResponseMessage("Success")
                .debitAccount(debitAccount)
                .currentBalance(defaultGoal.getSavingsBalance())
                .newBalance(newBalance)
                .externalReference(nipTransactionInterestEvent.getExternalReference())
                .savingsGoal(defaultGoal)
                .performedBy(appUserEntity)
                .creditAccount(savingsAccount)
                .build();
        savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
        defaultGoal.setSavingsBalance(newBalance);
        savingsGoalEntityDao.saveRecord(defaultGoal);
        log.info("Nip interest applied successfully.");
    }
}
