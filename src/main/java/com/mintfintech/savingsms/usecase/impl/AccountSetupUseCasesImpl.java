package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.AccountTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountGroupConstant;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TierLevelTypeConstant;
import com.mintfintech.savingsms.usecase.AccountSetupUseCases;
import com.mintfintech.savingsms.usecase.CreateSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.events.incoming.*;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@FieldDefaults(makeFinal = true)
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

    @Transactional
    @Override
    public void createMintAccount(MintAccountCreationEvent mintAccountCreationEvent) {
        Optional<MintAccountEntity> mintAccountEntityOptional = mintAccountEntityDao.findAccountByAccountId(mintAccountCreationEvent.getAccountId());
        if(!mintAccountEntityOptional.isPresent()){
            LocalDateTime dateCreated = LocalDateTime.parse(mintAccountCreationEvent.getDateCreated(), DateTimeFormatter.ISO_DATE_TIME);
            MintAccountEntity mintAccountEntity = MintAccountEntity.builder()
                    .accountId(mintAccountCreationEvent.getAccountId())
                    .accountType(AccountTypeConstant.valueOf(mintAccountCreationEvent.getAccountType()))
                    .name(mintAccountCreationEvent.getName()).build();
            mintAccountEntity.setDateCreated(dateCreated);
            mintAccountEntity = mintAccountEntityDao.saveRecord(mintAccountEntity);
            log.info("mint account created successfully: {}", mintAccountEntity.getAccountId());
            UserCreationEvent userCreationEvent = mintAccountCreationEvent.getUserCreationEvent();
            dateCreated = LocalDateTime.parse(userCreationEvent.getDateCreated(), DateTimeFormatter.ISO_DATE_TIME);
            String name = String.format("%s %s", userCreationEvent.getFirstName(), userCreationEvent.getLastName());
            AppUserEntity appUserEntity = AppUserEntity.builder()
                    .userId(userCreationEvent.getUserId())
                    .email(userCreationEvent.getEmail())
                    .name(name).phoneNumber(userCreationEvent.getPhoneNumber())
                    .primaryAccount(mintAccountEntity).build();
            appUserEntity.setDateCreated(dateCreated);
            appUserEntityDao.saveRecord(appUserEntity);
            log.info("User created successfully: {}", appUserEntity.getUserId());
            if(mintAccountEntity.getAccountType() == AccountTypeConstant.INDIVIDUAL) {
                //SavingsGoalEntity  savingsGoalEntity = createSavingsGoalUseCase.createDefaultSavingsGoal(mintAccountEntity, appUserEntity);
                //log.info("Customer Default saving goal created by Id: {}", savingsGoalEntity.getId());
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
            LocalDateTime dateCreated = LocalDateTime.now();
            if(!StringUtils.isEmpty(accountCreationEvent.getDateCreated())){
                dateCreated = LocalDateTime.parse(accountCreationEvent.getDateCreated(), DateTimeFormatter.ISO_DATE_TIME);
            }
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
            bankAccountEntity.setDateCreated(dateCreated);
            mintBankAccountEntityDao.saveRecord(bankAccountEntity);
        }
    }
    private TierLevelEntity getAccountTierLevel(String tierLevel) {
        TierLevelTypeConstant tierLevelType = TierLevelTypeConstant.valueOf(tierLevel);
        Optional<TierLevelEntity> optionalTierLevelEntity = tierLevelEntityDao.findByTierLevelType(tierLevelType);
        return optionalTierLevelEntity.orElseGet(() -> tierLevelEntityDao.getByTierLevelType(TierLevelTypeConstant.TIER_ONE));
    }

   /*@Override
    public void updateAccountTransactionLimit(AccountLimitUpdateEvent accountLimitUpdateEvent) {
        Optional<MintAccountEntity> mintAccountEntityOptional = mintAccountEntityDao.findAccountByAccountId(accountLimitUpdateEvent.getAccountId());
        if(!mintAccountEntityOptional.isPresent()) {
            return;
        }
        MintAccountEntity mintAccountEntity = mintAccountEntityOptional.get();
        mintAccountEntity.setBulletTransactionLimit(accountLimitUpdateEvent.getBulletLimitAmount());
        mintAccountEntity.setDailyTransactionLimit(accountLimitUpdateEvent.getDailyLimitAmount());
        mintAccountEntityDao.saveRecord(mintAccountEntity);
    }*/

    @Override
    public void updateNotificationPreference(NotificationPreferenceUpdateEvent preferenceUpdateEvent) {
        Optional<AppUserEntity> appUserEntityOptional = appUserEntityDao.findAppUserByUserId(preferenceUpdateEvent.getUserId());
        if(!appUserEntityOptional.isPresent()){
            return;
        }
        AppUserEntity appUserEntity = appUserEntityOptional.get();
        appUserEntity.setEmailNotificationEnabled(preferenceUpdateEvent.isEmailEnabled());
        appUserEntity.setSmsNotificationEnabled(preferenceUpdateEvent.isSmsEnabled());
        appUserEntity.setGcmNotificationEnabled(preferenceUpdateEvent.isGcmEnabled());
        appUserEntityDao.saveRecord(appUserEntity);
    }

    @Override
    public void updateBankAccountTierLevel(BankAccountTierUpgradeEvent tierUpgradeEvent) {
        Optional<MintBankAccountEntity> optionalMintBankAccountEntity = mintBankAccountEntityDao.findByAccountNumber(tierUpgradeEvent.getAccountNumber());
        if(!optionalMintBankAccountEntity.isPresent()) {
            return;
        }
        MintBankAccountEntity bankAccountEntity = optionalMintBankAccountEntity.get();
        TierLevelEntity tierLevelEntity = getAccountTierLevel(tierUpgradeEvent.getNewTierLevel());
        bankAccountEntity.setAccountTierLevel(tierLevelEntity);
        mintBankAccountEntityDao.saveRecord(bankAccountEntity);
        log.info("Account tier updated successfully.");
    }

    @Override
    public void updateUserDeviceNotificationId(String userId, String gcmTokenId) {
        Optional<AppUserEntity> appUserEntityOptional = appUserEntityDao.findAppUserByUserId(userId);
        if(!appUserEntityOptional.isPresent()){
            return;
        }
        AppUserEntity appUserEntity = appUserEntityOptional.get();
        appUserEntity.setDeviceGcmNotificationToken(gcmTokenId);
        appUserEntityDao.saveRecord(appUserEntity);
    }
}
