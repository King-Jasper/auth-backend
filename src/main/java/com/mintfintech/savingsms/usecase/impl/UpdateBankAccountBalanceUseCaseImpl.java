package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.corebankingservice.BalanceEnquiryResponseCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.incoming.AccountBalanceUpdateEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.MintAccountRecordRequestEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Slf4j
@Named
@AllArgsConstructor
public class UpdateBankAccountBalanceUseCaseImpl implements UpdateBankAccountBalanceUseCase {

    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final CoreBankingServiceClient coreBankingServiceClient;
    private final ApplicationEventService applicationEventService;

    @Override
    public void processBalanceUpdate(AccountBalanceUpdateEvent balanceUpdateEvent) {
        Optional<MintBankAccountEntity> accountEntityOptional;
        if(!StringUtils.isEmpty(balanceUpdateEvent.getAccountId())) {
            accountEntityOptional = mintBankAccountEntityDao.findByAccountId(balanceUpdateEvent.getAccountId());
        }else {
            accountEntityOptional = mintBankAccountEntityDao.findByAccountNumber(balanceUpdateEvent.getAccountNumber());
        }
        if(!accountEntityOptional.isPresent()) {
            String accountId = StringUtils.isEmpty(balanceUpdateEvent.getAccountId()) ? balanceUpdateEvent.getAccountNumber() : balanceUpdateEvent.getAccountId();
            MintAccountRecordRequestEvent requestEvent = MintAccountRecordRequestEvent.builder()
                    .topicNameSuffix("savings-service")
                    .accountIds(Collections.singletonList(accountId))
                    .build();
            applicationEventService.publishEvent(ApplicationEventService.EventType.MISSING_BANK_ACCOUNT_RECORD, new EventModel<>(requestEvent));
            return;
        }

        MintBankAccountEntity bankAccountEntity = accountEntityOptional.get();
        bankAccountEntity.setAvailableBalance(balanceUpdateEvent.getAvailableBalance());
        bankAccountEntity.setLedgerBalance(balanceUpdateEvent.getLedgerBalance());
        bankAccountEntity.setBalanceUpdateTime(LocalDateTime.now());
        mintBankAccountEntityDao.saveRecord(bankAccountEntity);
        //log.info("account balance updated: {},{}", bankAccountEntity.getAccountId(), balanceUpdateEvent.getAvailableBalance());
    }

    @Override
    public MintBankAccountEntity processBalanceUpdate(MintBankAccountEntity bankAccountEntity) {
        MsClientResponse<BalanceEnquiryResponseCBS> msClientResponse = coreBankingServiceClient.retrieveAccountBalance(bankAccountEntity.getAccountNumber());
        if(!msClientResponse.isSuccess()){
            return bankAccountEntity;
        }
        BalanceEnquiryResponseCBS enquiryResponseCBS = msClientResponse.getData();
        bankAccountEntity.setAvailableBalance(enquiryResponseCBS.getAvailableBalance());
        bankAccountEntity.setLedgerBalance(enquiryResponseCBS.getLedgerBalance());
        bankAccountEntity.setBalanceUpdateTime(LocalDateTime.now());
        bankAccountEntity = mintBankAccountEntityDao.saveRecord(bankAccountEntity);
        return bankAccountEntity;
    }

    /*
    @Override
    public void processBalanceUpdate(MintAccountEntity mintAccountEntity) {
        List<MintBankAccountEntity> bankAccountEntityList = mintBankAccountEntityDao.getAccountsByMintAccount(mintAccountEntity);
        for(MintBankAccountEntity bankAccountEntity: bankAccountEntityList) {
            if(bankAccountEntity.getAccountType() == BankAccountTypeConstant.SAVING) {
                new Thread(() -> {
                    processBalanceUpdate(bankAccountEntity);
                }).start();
            }else {
                processBalanceUpdate(bankAccountEntity);
            }
        }
    }*/
}
