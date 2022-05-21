package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.usecase.AccountSetupUseCases;
import com.mintfintech.savingsms.usecase.CreateSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.events.incoming.*;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;

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
    private CorporateUserEntityDao corporateUserEntityDao;

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
            Optional<AppUserEntity> userOpt = appUserEntityDao.findAppUserByUserId(userCreationEvent.getUserId());
            AppUserEntity appUserEntity = userOpt.orElseGet(() -> createUser(userCreationEvent));
            mintAccountEntity.setCreator(appUserEntity);
            mintAccountEntityDao.saveRecord(mintAccountEntity);
            if(mintAccountEntity.getAccountType() == AccountTypeConstant.INDIVIDUAL) {
                appUserEntity.setPrimaryAccount(mintAccountEntity);
                appUserEntityDao.saveRecord(appUserEntity);
            }
            log.info("User created successfully: {}", appUserEntity.getUserId());
        }
    }

    @Async
    @Override
    @SneakyThrows
    public void createMintUser(UserCreationEvent userCreationEvent) {
        Thread.sleep(500);
        if(appUserEntityDao.findAppUserByUserId(userCreationEvent.getUserId()).isPresent()) {
            return;
        }
        createUser(userCreationEvent);
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
                    .currency(currencyEntity)
                    .build();
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

    @Override
    public void updateBankAccountStatus(BankAccountStatusUpdateEvent accountStatusUpdateEvent) {
        Optional<MintBankAccountEntity> optionalMintBankAccountEntity = mintBankAccountEntityDao.findByAccountNumber(accountStatusUpdateEvent.getAccountNumber());
        if(!optionalMintBankAccountEntity.isPresent()) {
            return;
        }
        MintBankAccountEntity bankAccountEntity = optionalMintBankAccountEntity.get();
        bankAccountEntity.setAccountStatus(BankAccountStatusConstant.valueOf(accountStatusUpdateEvent.getStatus()));
        mintBankAccountEntityDao.saveRecord(bankAccountEntity);
    }

    @Override
    public void updateUserProfileDetails(UserDetailUpdateEvent updateEvent) {
        Optional<AppUserEntity> optionalAppUserEntity = appUserEntityDao.findAppUserByUserId(updateEvent.getUserId());
        if(!optionalAppUserEntity.isPresent()) {
            return;
        }
        AppUserEntity appUserEntity = optionalAppUserEntity.get();
        appUserEntity.setEmail(updateEvent.getEmail());
        appUserEntity.setPhoneNumber(updateEvent.getPhoneNumber());
        appUserEntityDao.saveRecord(appUserEntity);
    }

    private AppUserEntity createUser(UserCreationEvent userCreationEvent) {
        String name = String.format("%s %s", StringUtils.capitalize(userCreationEvent.getFirstName().toLowerCase()), StringUtils.capitalize(userCreationEvent.getLastName().toLowerCase()));
        LocalDateTime dateCreated = LocalDateTime.parse(userCreationEvent.getDateCreated(), DateTimeFormatter.ISO_DATE_TIME);
        AppUserEntity appUserEntity = AppUserEntity.builder()
                .username(userCreationEvent.getUsername())
                .userId(userCreationEvent.getUserId())
                .email(userCreationEvent.getEmail())
                .name(name)
                .phoneNumber(userCreationEvent.getPhoneNumber())
                .build();
        appUserEntity.setDateCreated(dateCreated);
        return appUserEntityDao.saveRecord(appUserEntity);
    }

    @Async
    @SneakyThrows
    @Override
    public void createOrUpdateCorporateUser(CorporateUserDetailEvent corporateUserDetailEvent) {
        log.info("Corporate user details event - {}", corporateUserDetailEvent.toString());
        String accountId = corporateUserDetailEvent.getAccountId();
        String userId = corporateUserDetailEvent.getUserId();
        Optional<CorporateUserEntity> optional = corporateUserEntityDao.findRecordByAccountIdAndUserId(accountId, userId);
        if(optional.isPresent()) {
            log.info("1. Corporate user details event - {}", corporateUserDetailEvent);
            CorporateUserEntity corporateUserEntity = optional.get();
            corporateUserEntity.setDirector(corporateUserDetailEvent.isDirector());
            corporateUserEntity.setUserRole(corporateUserDetailEvent.getRoleName());
            corporateUserEntityDao.saveRecord(corporateUserEntity);
        } else {
            log.info("1. Corporate user details event - {}", corporateUserDetailEvent);
            Optional<MintAccountEntity> accountEntityOpt = mintAccountEntityDao.findAccountByAccountId(accountId);
            Optional<AppUserEntity> appUserEntityOpt = appUserEntityDao.findAppUserByUserId(userId);
            if(!accountEntityOpt.isPresent() || !appUserEntityOpt.isPresent()) {
                Thread.sleep(1000);
                accountEntityOpt = mintAccountEntityDao.findAccountByAccountId(accountId);
                appUserEntityOpt = appUserEntityDao.findAppUserByUserId(userId);
                if(!accountEntityOpt.isPresent() || !appUserEntityOpt.isPresent()) {
                    log.info("Missing record accountId - {} or userId - {}", accountId, userId);
                    return;
                }
            }
            AppUserEntity appUserEntity = appUserEntityOpt.get();
            CorporateUserEntity corporateUserEntity = CorporateUserEntity.builder()
                    .appUser(appUserEntity)
                    .corporateAccount(accountEntityOpt.get())
                    .director(corporateUserDetailEvent.isDirector())
                    .userRole(corporateUserDetailEvent.getRoleName())
                    .build();
            corporateUserEntityDao.saveRecord(corporateUserEntity);
        }
    }
}
