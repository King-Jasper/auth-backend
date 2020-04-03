package com.mintfintech.savingsms.infrastructure.messaging;

import com.google.gson.Gson;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.incoming.AccountBalanceUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Sun, 16 Feb, 2020
 */
@Slf4j
@Named
public class CoreBankingMSEventListener {

    private final String ACCOUNT_BALANCE_UPDATE_EVENT = "com.mintfintech.core-banking-service.events.bank-account-balance-update";

    private UpdateBankAccountBalanceUseCase updateBankAccountBalanceUseCase;
    private Gson gson;
    public CoreBankingMSEventListener(UpdateBankAccountBalanceUseCase updateBankAccountBalanceUseCase, Gson gson) {
        this.updateBankAccountBalanceUseCase = updateBankAccountBalanceUseCase;
        this.gson = gson;
    }


    @KafkaListener(topics = {ACCOUNT_BALANCE_UPDATE_EVENT})
    public void listenForAccountBalanceUpdate(String payload) {
        log.info("event:{} ; {}", ACCOUNT_BALANCE_UPDATE_EVENT, payload);
        AccountBalanceUpdateEvent event = gson.fromJson(payload, AccountBalanceUpdateEvent.class);
        updateBankAccountBalanceUseCase.processBalanceUpdate(event);
    }

}
