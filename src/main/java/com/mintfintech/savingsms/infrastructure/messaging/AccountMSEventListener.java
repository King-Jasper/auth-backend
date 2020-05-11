package com.mintfintech.savingsms.infrastructure.messaging;

import com.google.gson.Gson;

import com.mintfintech.savingsms.usecase.AccountSetupUseCases;
import com.mintfintech.savingsms.usecase.data.events.incoming.AccountLimitUpdateEvent;
import com.mintfintech.savingsms.usecase.data.events.incoming.BankAccountTierUpgradeEvent;
import com.mintfintech.savingsms.usecase.data.events.incoming.MintAccountCreationEvent;
import com.mintfintech.savingsms.usecase.data.events.incoming.MintBankAccountCreationEvent;
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

    public AccountMSEventListener(Gson gson,  AccountSetupUseCases accountSetupUseCases) {
        this.gson = gson;
        this.accountSetupUseCases = accountSetupUseCases;
    }
    private final String MINT_ACCOUNT_CREATION_EVENT = "com.mintfintech.accounts-service.events.mint-account-creation";
    private final String MINT_BANK_ACCOUNT_CREATION_EVENT = "com.mintfintech.accounts-service.events.bank-account-creation";
    private final String MINT_ACCOUNT_LIMIT_UPDATE_EVENT = "com.mintfintech.accounts-service.events.mint-account-limit-update";
    private final String MINT_BANK_ACCOUNT_TIER_UPDATE_EVENT = "com.mintfintech.accounts-service.events.bank-account-tier-level-upgrade";


    @KafkaListener(topics = {MINT_ACCOUNT_CREATION_EVENT})
    public void listenForAccountCreation(String payload) {
        log.info("mint account creation: {}", payload);
        MintAccountCreationEvent mintAccountCreationEvent = gson.fromJson(payload, MintAccountCreationEvent.class);
        accountSetupUseCases.createMintAccount(mintAccountCreationEvent);
    }

    @KafkaListener(topics = {MINT_BANK_ACCOUNT_CREATION_EVENT})
    public void listenForMintBankCreation(String payload) {
        log.info("bank account creation: {}", payload);
        MintBankAccountCreationEvent event = gson.fromJson(payload, MintBankAccountCreationEvent.class);
        accountSetupUseCases.createIndividualBankAccount(event);
    }

   /* @KafkaListener(topics = {MINT_ACCOUNT_LIMIT_UPDATE_EVENT})
    public void listenForMintAccountLimitUpdate(String payload) {
        log.info("account limit update : {}", payload);
        AccountLimitUpdateEvent event = gson.fromJson(payload, AccountLimitUpdateEvent.class);
        accountSetupUseCases.updateAccountTransactionLimit(event);
    }*/

    @KafkaListener(topics = {MINT_BANK_ACCOUNT_TIER_UPDATE_EVENT})
    public void listenForMintBankAccountTierUpdate(String payload) {
        log.info("account limit update : {}", payload);
        BankAccountTierUpgradeEvent event = gson.fromJson(payload, BankAccountTierUpgradeEvent.class);
        accountSetupUseCases.updateBankAccountTierLevel(event);
    }

}
