package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.domain.models.corebankingservice.TransactionStatusRequestCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.TransactionStatusResponseCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.usecase.UpdateTransactionUseCase;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Created by jnwanya on
 * Sun, 02 Aug, 2020
 */
@Named
@AllArgsConstructor
public class UpdateTransactionUseCaseImpl implements UpdateTransactionUseCase {

    private final CoreBankingServiceClient coreBankingServiceClient;
    private final SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    private final SystemIssueLogService systemIssueLogService;
    private final SavingsGoalEntityDao savingsGoalEntityDao;

    @Override
    public void updatePendingTransaction() {
       int size = 10;
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
       List<SavingsGoalTransactionEntity> transactionList = savingsGoalTransactionEntityDao.getTransactionByTypeAndStatusBeforeTime(TransactionTypeConstant.CREDIT, TransactionStatusConstant.PENDING, tenMinutesAgo , size);
       for(SavingsGoalTransactionEntity transactionEntity: transactionList) {

           TransactionStatusRequestCBS requestCBS = TransactionStatusRequestCBS.builder()
                   .amount(transactionEntity.getTransactionAmount())
                   .transactionDate(transactionEntity.getDateCreated().format(DateTimeFormatter.ISO_LOCAL_DATE))
                   .transactionReference(transactionEntity.getTransactionReference())
                   .nipTransfer(false)
                   .build();
           MsClientResponse<TransactionStatusResponseCBS> msClientResponse = coreBankingServiceClient.reQueryTransactionStatus(requestCBS);
           if(!msClientResponse.isSuccess()) {
               systemIssueLogService.logIssue("TransactionRequest Failed", "Transaction requery failure", msClientResponse.toString());
               continue;
           }
           TransactionStatusResponseCBS responseCBS = msClientResponse.getData();
           transactionEntity.setTransactionResponseCode(responseCBS.getResponseCode());
           transactionEntity.setTransactionResponseMessage(responseCBS.getResponseMessage());
           if("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
               transactionEntity.setTransactionStatus(TransactionStatusConstant.SUCCESSFUL);
           }else if(isPendingTransaction(responseCBS.getResponseCode(), responseCBS.getResponseStatus())) {
               transactionEntity.setTransactionStatus(TransactionStatusConstant.PENDING);
           }else {
               transactionEntity.setTransactionStatus(TransactionStatusConstant.FAILED);
           }
           savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
           if(transactionEntity.getTransactionStatus() == TransactionStatusConstant.SUCCESSFUL) {
               SavingsGoalEntity savingsGoal = savingsGoalEntityDao.getRecordById(transactionEntity.getSavingsGoal().getId());
               savingsGoal.setSavingsBalance(savingsGoal.getSavingsBalance().add(transactionEntity.getTransactionAmount()));
               savingsGoalEntityDao.saveRecord(savingsGoal);
               transactionEntity.setNewBalance(savingsGoal.getSavingsBalance());
               savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
           }
       }
    }
    private boolean isUnsuccessfulStatus(String status) {
        return "failed".equalsIgnoreCase(status) || "reversed".equalsIgnoreCase(status);
    }

    private boolean isPendingTransaction(String code, String status) {
        return !isUnsuccessfulStatus(status) && ("02".equalsIgnoreCase(code) || "96".equalsIgnoreCase(code) || "91".equalsIgnoreCase(code));
    }

}
