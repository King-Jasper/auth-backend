package com.mintfintech.savingsms.infrastructure.messaging;

import com.google.gson.Gson;

import com.mintfintech.savingsms.usecase.AccountSetupUseCases;
import com.mintfintech.savingsms.usecase.data.events.incoming.*;
import com.mintfintech.savingsms.usecase.data.events.outgoing.EmploymentInfoUpdateEvent;
import com.mintfintech.savingsms.usecase.features.loan.UpdateEmploymentInfoUseCase;
import com.mintfintech.savingsms.usecase.features.referral_savings.CreateReferralRewardUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
@Slf4j
@Named
public class AccountMSEventListener {
    private final Gson gson;
    private final AccountSetupUseCases accountSetupUseCases;
    private final CreateReferralRewardUseCase createReferralRewardUseCase;
    private final UpdateEmploymentInfoUseCase updateEmploymentInfoUseCase;

    public AccountMSEventListener(Gson gson, AccountSetupUseCases accountSetupUseCases, CreateReferralRewardUseCase createReferralRewardUseCase, UpdateEmploymentInfoUseCase updateEmploymentInfoUseCase) {
        this.gson = gson;
        this.accountSetupUseCases = accountSetupUseCases;
        this.createReferralRewardUseCase = createReferralRewardUseCase;
        this.updateEmploymentInfoUseCase = updateEmploymentInfoUseCase;
    }
    private final String MINT_ACCOUNT_CREATION_EVENT = "com.mintfintech.accounts-service.events.mint-account-creation";
    private final String MINT_BANK_ACCOUNT_CREATION_EVENT = "com.mintfintech.accounts-service.events.bank-account-creation";
    private final String MINT_ACCOUNT_LIMIT_UPDATE_EVENT = "com.mintfintech.accounts-service.events.mint-account-limit-update";
    private final String MINT_BANK_ACCOUNT_TIER_UPDATE_EVENT = "com.mintfintech.accounts-service.events.bank-account-tier-level-upgrade";
    private final String MINT_NOTIFICATION_PREFERENCE_UPDATE_EVENT = "com.mintfintech.accounts-service.events.user-notification-preference-update";
    private final String CUSTOMER_DEVICE_CHANGE_EVENT = "com.mintfintech.accounts-service.events.user.device-change";
    private final String GCM_NOTIFICATION_DETAIL_EVENT = "com.mintfintech.accounts-service.events.user-gcm-detail-broadcast";
    private final String CUSTOMER_REFERRAL_EVENT = "com.mintfintech.accounts-service.events.customer-referral-info";
    private final String MINT_BANK_ACCOUNT_STATUS_UPDATE_EVENT = "com.mintfintech.accounts-service.events.bank-account-status-update";
    private final String USER_PROFILE_UPDATE_EVENT = "com.mintfintech.accounts-service.events.user-profile-update";
    private final String EMPLOYMENT_INFORMATION_UPDATE_EVENT = "com.mintfintech.savings-service.events.employment-info-update";
    private final String MINT_CORPORATE_USER_DETAILS = "com.mintfintech.accounts-service.events.corporate-user-detail";
    private final String MINT_USER_CREATION_EVENT = "com.mintfintech.accounts-service.events.user-creation";
    private final String ACCOUNT_NAME_UPDATE_EVENT = "com.mintfintech.accounts-service.events.account-name-update";
    private final String CORPORATE_TYPE_CHANGE_EVENT = "com.mintfintech.accounts-service.events.corporate-type-update";


    @KafkaListener(topics = {MINT_ACCOUNT_CREATION_EVENT, MINT_ACCOUNT_CREATION_EVENT+".savings-service"})
    public void listenForAccountCreation(String payload) {
       // log.info("mint account creation: {}", payload);
        MintAccountCreationEvent mintAccountCreationEvent = gson.fromJson(payload, MintAccountCreationEvent.class);
        accountSetupUseCases.createMintAccount(mintAccountCreationEvent);
    }

    @KafkaListener(topics = {MINT_BANK_ACCOUNT_CREATION_EVENT, MINT_BANK_ACCOUNT_CREATION_EVENT+".savings-service"})
    public void listenForMintBankCreation(String payload) {
       // log.info("bank account creation: {}", payload);
        MintBankAccountCreationEvent event = gson.fromJson(payload, MintBankAccountCreationEvent.class);
        accountSetupUseCases.createIndividualBankAccount(event);
    }

    @KafkaListener(topics = {MINT_USER_CREATION_EVENT, MINT_USER_CREATION_EVENT+".savings-service"})
    public void listenForUserCreation(String payload) {
        UserCreationEvent event = gson.fromJson(payload, UserCreationEvent.class);
        accountSetupUseCases.createMintUser(event);
    }

    @KafkaListener(topics = {MINT_CORPORATE_USER_DETAILS, MINT_CORPORATE_USER_DETAILS+".savings-service"})
    public void listenForCorporateUserCreation(String payload) {
        CorporateUserDetailEvent event = gson.fromJson(payload, CorporateUserDetailEvent.class);
        accountSetupUseCases.createOrUpdateCorporateUser(event);
    }

    @KafkaListener(topics = {MINT_NOTIFICATION_PREFERENCE_UPDATE_EVENT})
    public void listenForUserNotificationPreferenceUpdate(String payload) {
        //log.info("notification preference update: {}", payload);
        NotificationPreferenceUpdateEvent event = gson.fromJson(payload, NotificationPreferenceUpdateEvent.class);
        accountSetupUseCases.updateNotificationPreference(event);
    }

   /* @KafkaListener(topics = {MINT_ACCOUNT_LIMIT_UPDATE_EVENT})
    public void listenForMintAccountLimitUpdate(String payload) {
        log.info("account limit update : {}", payload);
        AccountLimitUpdateEvent event = gson.fromJson(payload, AccountLimitUpdateEvent.class);
        accountSetupUseCases.updateAccountTransactionLimit(event);
    }*/

    @KafkaListener(topics = {MINT_BANK_ACCOUNT_TIER_UPDATE_EVENT})
    public void listenForMintBankAccountTierUpdate(String payload) {
        //log.info("account limit update : {}", payload);
        BankAccountTierUpgradeEvent event = gson.fromJson(payload, BankAccountTierUpgradeEvent.class);
        accountSetupUseCases.updateBankAccountTierLevel(event);
    }

    @KafkaListener(topics = {CUSTOMER_DEVICE_CHANGE_EVENT, GCM_NOTIFICATION_DETAIL_EVENT})
    public void listenForCustomerDeviceChange(String payload) {
        //log.info("customer push notification detail: {}", payload);
        CustomerDeviceChangeEvent deviceChangeEvent = gson.fromJson(payload, CustomerDeviceChangeEvent.class);
        accountSetupUseCases.updateUserDeviceNotificationId(deviceChangeEvent.getCustomerId(), deviceChangeEvent.getDeviceNotificationId());
    }

    @KafkaListener(topics = {CUSTOMER_REFERRAL_EVENT})
    public void listenForCustomerReferral(String payload) {
        CustomerReferralEvent event = gson.fromJson(payload, CustomerReferralEvent.class);
        createReferralRewardUseCase.processCustomerReferralReward(event);
    }

    @KafkaListener(topics = {MINT_BANK_ACCOUNT_STATUS_UPDATE_EVENT})
    public void listenForMintBankAccountStatusUpdate(String payload) {
        BankAccountStatusUpdateEvent event = gson.fromJson(payload, BankAccountStatusUpdateEvent.class);
        accountSetupUseCases.updateBankAccountStatus(event);
    }

    @KafkaListener(topics = {USER_PROFILE_UPDATE_EVENT})
    public void listenForUserProfileUpdate(String payload) {
        UserDetailUpdateEvent event = gson.fromJson(payload, UserDetailUpdateEvent.class);
        accountSetupUseCases.updateUserProfileDetails(event);
    }

    @KafkaListener(topics = {EMPLOYMENT_INFORMATION_UPDATE_EVENT})
    public void listenForEmploymentInfoUpdate(String payload) {
        EmploymentInfoUpdateEvent event = gson.fromJson(payload, EmploymentInfoUpdateEvent.class);
        updateEmploymentInfoUseCase.updateCustomerEmploymentInformation(event);
    }

    @KafkaListener(topics = {ACCOUNT_NAME_UPDATE_EVENT})
    public void listenForUpdateOnProfileName(String payload) {
        ProfileNameUpdateEvent event = gson.fromJson(payload, ProfileNameUpdateEvent.class);
        accountSetupUseCases.updateNameOnAccount(event);
    }
    @KafkaListener(topics = {CORPORATE_TYPE_CHANGE_EVENT})
    public void listenForCorporateTypeChange(String payload) {
        BusinessTypeChangeEvent event = gson.fromJson(payload, BusinessTypeChangeEvent.class);
        accountSetupUseCases.updateCorporateType(event);
    }

}
