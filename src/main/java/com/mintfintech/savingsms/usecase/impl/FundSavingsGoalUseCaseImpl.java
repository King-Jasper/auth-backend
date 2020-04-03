package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.MintFundTransferRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.usecase.FundSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.MintTransactionEvent;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;

import javax.inject.Named;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Named
@AllArgsConstructor
public class FundSavingsGoalUseCaseImpl implements FundSavingsGoalUseCase {

    private CoreBankingServiceClient coreBankingServiceClient;
    private SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private MintBankAccountEntityDao mintBankAccountEntityDao;
    private ApplicationEventService applicationEventService;
    private UpdateBankAccountBalanceUseCase updateAccountBalanceUseCase;

    @Override
    public SavingsGoalFundingResponse fundSavingGoal(MintBankAccountEntity debitAccount, AppUserEntity appUserEntity, SavingsGoalEntity savingsGoal, BigDecimal amount) {

        MintBankAccountEntity creditAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(debitAccount.getMintAccount(), BankAccountTypeConstant.SAVING);
        SavingsGoalTransactionEntity transactionEntity = SavingsGoalTransactionEntity.builder()
                .transactionAmount(amount)
                .transactionReference(savingsGoalTransactionEntityDao.generateTransactionReference())
                .debitAccount(debitAccount)
                .creditAccount(creditAccount)
                .transactionType(TransactionTypeConstant.CREDIT)
                .transactionStatus(TransactionStatusConstant.PENDING)
                .savingsGoal(savingsGoal)
                .performedBy(appUserEntity)
                .currentBalance(savingsGoal.getSavingsBalance())
                .build();
        transactionEntity = savingsGoalTransactionEntityDao.saveRecord(transactionEntity);

        BigDecimal balanceBeforeTransaction = debitAccount.getAvailableBalance();

        MintFundTransferRequestCBS transferRequestCBS = MintFundTransferRequestCBS.builder()
                .amount(amount)
                .creditAccountNumber(creditAccount.getAccountNumber())
                .debitAccountNumber(debitAccount.getAccountNumber())
                .narration("Savings - "+savingsGoal.getGoalId())
                .transactionReference(transactionEntity.getTransactionReference())
                .build();
        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processMintFundTransfer(transferRequestCBS);
        transactionEntity = processResponse(transactionEntity, msClientResponse);

        SavingsGoalFundingResponse fundingResponse = SavingsGoalFundingResponse.builder()
                .responseCode(transactionEntity.getTransactionResponseCode())
                .responseMessage(transactionEntity.getTransactionResponseMessage())
                .transactionReference(transactionEntity.getTransactionReference())
                .build();
        if(transactionEntity.getTransactionStatus() == TransactionStatusConstant.SUCCESSFUL) {
            savingsGoal.setSavingsBalance(savingsGoal.getSavingsBalance().add(amount));
            savingsGoalEntityDao.saveRecord(savingsGoal);
            transactionEntity.setNewBalance(savingsGoal.getSavingsBalance());
            savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
            debitAccount = updateAccountBalanceUseCase.processBalanceUpdate(debitAccount);
            BigDecimal newBalance = debitAccount.getAvailableBalance();
            createTransactionLog(transactionEntity, balanceBeforeTransaction, newBalance);
            fundingResponse.setResponseCode("00");
        }else if(transactionEntity.getTransactionStatus() == TransactionStatusConstant.PENDING) {
            fundingResponse.setResponseCode("01");
        }else {
            fundingResponse.setResponseCode("02");
        }
        fundingResponse.setSavingsBalance(savingsGoal.getSavingsBalance());
        return fundingResponse;
    }


    private SavingsGoalTransactionEntity processResponse(SavingsGoalTransactionEntity transactionEntity, MsClientResponse<FundTransferResponseCBS> msClientResponse) {
        if(!msClientResponse.isSuccess()){
            if(msClientResponse.getStatusCode() == HttpStatus.BAD_REQUEST.value()  || msClientResponse.getStatusCode() == HttpStatus.CONFLICT.value()){
                String message = "Transaction validation failed";
                transactionEntity.setTransactionResponseCode("-1");
                transactionEntity.setTransactionResponseMessage(message);
                transactionEntity.setTransactionStatus(TransactionStatusConstant.FAILED);
                return savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
            }
            transactionEntity.setTransactionResponseCode("-1");
            transactionEntity.setTransactionResponseMessage("Transaction status not yet confirmed.");
            transactionEntity.setTransactionStatus(TransactionStatusConstant.PENDING);
            return savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
        }
        FundTransferResponseCBS responseCBS = msClientResponse.getData();
        String code = responseCBS.getResponseCode();
        transactionEntity.setTransactionResponseCode(code);
        transactionEntity.setTransactionResponseMessage(responseCBS.getResponseMessage());
        transactionEntity.setExternalReference(responseCBS.getBankOneReference());
        savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
        if(code.equalsIgnoreCase("91")){
            transactionEntity.setTransactionResponseMessage("Transaction status pending. Please check your balance before trying again.");
        }
        if("00".equalsIgnoreCase(code)) {
            transactionEntity.setTransactionStatus(TransactionStatusConstant.SUCCESSFUL);
        }else {
            transactionEntity.setTransactionStatus(TransactionStatusConstant.FAILED);
        }
        return savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
    }


    private void createTransactionLog(SavingsGoalTransactionEntity savingsGoalTransactionEntity, BigDecimal openingBalance, BigDecimal currentBalance) {
        String description = "Savings Goal funding - "+savingsGoalTransactionEntity.getSavingsGoal().getGoalId();
        MintTransactionEvent transactionPayload = MintTransactionEvent.builder()
                .balanceAfterTransaction(currentBalance)
                .balanceBeforeTransaction(openingBalance)
                .transactionAmount(savingsGoalTransactionEntity.getTransactionAmount())
                .transactionType(TransactionTypeConstant.DEBIT.name())
                .category("SAVINGS_GOAL")
                .debitAccountId(savingsGoalTransactionEntity.getDebitAccount().getId())
                .description(description)
                .externalReference(savingsGoalTransactionEntity.getExternalReference())
                .internalReference(savingsGoalTransactionEntity.getTransactionReference())
                .spendingTagId(0)
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.MINT_TRANSACTION_LOG, new EventModel<>(transactionPayload));
    }
}
