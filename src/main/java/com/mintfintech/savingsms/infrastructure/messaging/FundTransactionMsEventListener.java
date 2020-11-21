package com.mintfintech.savingsms.infrastructure.messaging;

import com.google.gson.Gson;
import com.mintfintech.savingsms.usecase.data.events.incoming.AccountCreditEvent;
import com.mintfintech.savingsms.usecase.data.events.incoming.NipTransactionInterestEvent;
import com.mintfintech.savingsms.usecase.ApplyNipTransactionInterestUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import javax.inject.Named;

/**
 * Created by jnwanya on
 * Mon, 13 Apr, 2020
 */
@Slf4j
@Named
public class FundTransactionMsEventListener {
    private Gson gson;
    private ApplyNipTransactionInterestUseCase applyNipTransactionInterestUseCase;

    public FundTransactionMsEventListener(Gson gson, ApplyNipTransactionInterestUseCase applySavingsInterestUseCase) {
        this.applyNipTransactionInterestUseCase = applySavingsInterestUseCase;
        this.gson = gson;
    }
    private final String INTEREST_NIP_TRANSACTION = "com.mintfintech.savings-service.events.interest-nip-transaction";


    @KafkaListener(topics = {INTEREST_NIP_TRANSACTION})
    public void listenForInterestEligibleNipTransaction(String payload) {
        log.info("mint account creation: {}", payload);
        NipTransactionInterestEvent nipTransactionInterestEvent = gson.fromJson(payload, NipTransactionInterestEvent.class);
        applyNipTransactionInterestUseCase.processNipInterest(nipTransactionInterestEvent);
    }



}
