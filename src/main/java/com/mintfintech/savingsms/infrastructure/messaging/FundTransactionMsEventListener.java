package com.mintfintech.savingsms.infrastructure.messaging;

import com.google.gson.Gson;
import com.mintfintech.savingsms.usecase.features.roundup_savings.ProcessRoundUpSavingsUseCase;
import com.mintfintech.savingsms.usecase.data.events.incoming.MintTransactionPayload;
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
    private final Gson gson;
    private final ApplyNipTransactionInterestUseCase applyNipTransactionInterestUseCase;
    private final ProcessRoundUpSavingsUseCase processRoundUpSavingsUseCase;

    public FundTransactionMsEventListener(Gson gson, ApplyNipTransactionInterestUseCase applySavingsInterestUseCase,
                                          ProcessRoundUpSavingsUseCase processRoundUpSavingsUseCase) {
        this.applyNipTransactionInterestUseCase = applySavingsInterestUseCase;
        this.gson = gson;
        this.processRoundUpSavingsUseCase = processRoundUpSavingsUseCase;
    }
    private final String INTEREST_NIP_TRANSACTION = "com.mintfintech.savings-service.events.interest-nip-transaction";
    private final String TRANSACTION_LOG_TOPIC = "com.mintfintech.fund-transaction-service.events.transaction-log";


    /*@KafkaListener(topics = {INTEREST_NIP_TRANSACTION})
    public void listenForInterestEligibleNipTransaction(String payload) {
        log.info("mint account creation: {}", payload);
        NipTransactionInterestEvent nipTransactionInterestEvent = gson.fromJson(payload, NipTransactionInterestEvent.class);
        applyNipTransactionInterestUseCase.processNipInterest(nipTransactionInterestEvent);
    }*/

    @KafkaListener(topics = {TRANSACTION_LOG_TOPIC})
    public void listenerForTransactionLog(String payload) {
        // log.info("mint transaction log: {}", payload);
        MintTransactionPayload transactionPayload = gson.fromJson(payload, MintTransactionPayload.class);
        processRoundUpSavingsUseCase.processTransactionForRoundUpSavings(transactionPayload);
    }

}
