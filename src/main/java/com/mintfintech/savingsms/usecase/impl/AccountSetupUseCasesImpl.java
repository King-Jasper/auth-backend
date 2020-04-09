package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.AccountTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountGroupConstant;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TierLevelTypeConstant;
import com.mintfintech.savingsms.usecase.AccountSetupUseCases;
import com.mintfintech.savingsms.usecase.CreateSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.events.incoming.AccountLimitUpdateEvent;
import com.mintfintech.savingsms.usecase.data.events.incoming.MintAccountCreationEvent;
import com.mintfintech.savingsms.usecase.data.events.incoming.MintBankAccountCreationEvent;
import com.mintfintech.savingsms.usecase.data.events.incoming.UserCreationEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Slf4j
@Named
@AllArgsConstructor
public class AccountSetupUseCasesImpl implements AccountSetupUseCases {

    private MintAccountEntityDao mintAccountEntityDao;
    private MintBankAccountEntityDao mintBankAccountEntityDao;
    private AppUserEntityDao appUserEntityDao;
    private CurrencyEntityDao currencyEntityDao;
    private TierLevelEntityDao tierLevelEntityDao;
    private CreateSavingsGoalUseCase createSavingsGoalUseCase;

    @Override
    public void createMintAccount(MintAccountCreationEvent mintAccountCreationEvent) {
        Optional<MintAccountEntity> mintAccountEntityOptional = mintAccountEntityDao.findAccountByAccountId(mintAccountCreationEvent.getAccountId());
        if(!mintAccountEntityOptional.isPresent()){
            MintAccountEntity mintAccountEntity = MintAccountEntity.builder()
                    .accountId(mintAccountCreationEvent.getAccountId())
                    .accountType(AccountTypeConstant.valueOf(mintAccountCreationEvent.getAccountType()))
                    .name(mintAccountCreationEvent.getName()).build();
            mintAccountEntity = mintAccountEntityDao.saveRecord(mintAccountEntity);
            log.info("mint account created successfully: {}", mintAccountEntity.getAccountId());
            UserCreationEvent userCreationEvent = mintAccountCreationEvent.getUserCreationEvent();
            String name = String.format("%s %s", userCreationEvent.getFirstName(), userCreationEvent.getLastName());
            AppUserEntity appUserEntity = AppUserEntity.builder()
                    .userId(userCreationEvent.getUserId())
                    .email(userCreationEvent.getEmail())
                    .name(name).phoneNumber(userCreationEvent.getPhoneNumber())
                    .primaryAccount(mintAccountEntity).build();
            appUserEntityDao.saveRecord(appUserEntity);
            log.info("User created successfully: {}", appUserEntity.getUserId());
            if(mintAccountEntity.getAccountType() == AccountTypeConstant.INDIVIDUAL) {
                SavingsGoalEntity  savingsGoalEntity = createSavingsGoalUseCase.createDefaultSavingsGoal(mintAccountEntity, appUserEntity);
                log.info("Customer Default saving goal created by Id: {}", savingsGoalEntity.getId());
            }
        }
    }

    @Override
    public void createIndividualBankAccount(MintBankAccountCreationEvent accountCreationEvent) {
        Optional<MintAccountEntity> mintAccountEntityOptional = mintAccountEntityDao.findAccountByAccountId(accountCreationEvent.getMintAccountId());
        if(!mintAccountEntityOptional.isPresent()) {
            log.error("Mint account information for bank account does not exist: {}", accountCreationEvent.getMintAccountId());
            return;
        }
        MintAccountEntity mintAccountEntity = mintAccountEntityOptional.get();
        String accountId = accountCreationEvent.getAccountId();
        Optional<MintBankAccountEntity> bankAccountEntityOptional = mintBankAccountEntityDao.findByAccountId(accountId);
        CurrencyEntity currencyEntity = currencyEntityDao.getByCode(accountCreationEvent.getCurrencyCode());
        if(!bankAccountEntityOptional.isPresent()) {
            TierLevelEntity accountTier = getAccountTierLevel(accountCreationEvent.getAccountTier());
            MintBankAccountEntity bankAccountEntity = MintBankAccountEntity.builder()
                    .accountGroup(BankAccountGroupConstant.valueOf(accountCreationEvent.getAccountGroup()))
                    .accountType(BankAccountTypeConstant.valueOf(accountCreationEvent.getAccountType()))
                    .accountId(accountId)
                    .accountName(accountCreationEvent.getAccountName())
                    .accountNumber(accountCreationEvent.getAccountNumber())
                    .accountTierLevel(accountTier)
                    .dailyTransactionLimit(BigDecimal.valueOf(accountCreationEvent.getDailyTransactionLimit()))
                    .mintAccount(mintAccountEntity)
                    .currency(currencyEntity).build();
            mintBankAccountEntityDao.saveRecord(bankAccountEntity);
        }
    }
    private TierLevelEntity getAccountTierLevel(String tierLevel) {
        TierLevelTypeConstant tierLevelType = TierLevelTypeConstant.valueOf(tierLevel);
        Optional<TierLevelEntity> optionalTierLevelEntity = tierLevelEntityDao.findByTierLevelType(tierLevelType);
        return optionalTierLevelEntity.orElseGet(() -> tierLevelEntityDao.getByTierLevelType(TierLevelTypeConstant.TIER_ONE));
    }

    @Override
    public void updateAccountTransactionLimit(AccountLimitUpdateEvent accountLimitUpdateEvent) {
        Optional<MintAccountEntity> mintAccountEntityOptional = mintAccountEntityDao.findAccountByAccountId(accountLimitUpdateEvent.getAccountId());
        if(!mintAccountEntityOptional.isPresent()) {
            return;
        }
        MintAccountEntity mintAccountEntity = mintAccountEntityOptional.get();
        mintAccountEntity.setBulletTransactionLimit(accountLimitUpdateEvent.getBulletLimitAmount());
        mintAccountEntity.setDailyTransactionLimit(accountLimitUpdateEvent.getDailyLimitAmount());
        mintAccountEntityDao.saveRecord(mintAccountEntity);
    }
}
