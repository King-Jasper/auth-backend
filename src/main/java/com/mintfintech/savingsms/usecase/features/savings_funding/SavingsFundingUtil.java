package com.mintfintech.savingsms.usecase.features.savings_funding;

import com.mintfintech.savingsms.domain.dao.SavingsGoalTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import org.springframework.http.HttpStatus;
import javax.inject.Named;

/**
 * Created by jnwanya on
 * Wed, 13 Jan, 2021
 */
@Named
public class SavingsFundingUtil {

    private final SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    public SavingsFundingUtil(SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao) {
        this.savingsGoalTransactionEntityDao = savingsGoalTransactionEntityDao;
    }

    public String constructFundingNarration(SavingsGoalEntity savingsGoalEntity) {
        String narration = String.format("SGF-%s %s", savingsGoalEntity.getGoalId(), savingsGoalEntity.getName());
        if(narration.length() > 61) {
            return narration.substring(0, 60);
        }
        return narration;
    }

    public SavingsGoalTransactionEntity processFundingTransactionResponse(SavingsGoalTransactionEntity transactionEntity, MsClientResponse<FundTransferResponseCBS> msClientResponse) {
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
        /*if(code.equalsIgnoreCase("91")){
            transactionEntity.setTransactionResponseMessage("Transaction status pending. Please check your balance before trying again.");
        }*/
        if("00".equalsIgnoreCase(code)) {
            transactionEntity.setTransactionStatus(TransactionStatusConstant.SUCCESSFUL);
        }else {
            transactionEntity.setTransactionStatus(TransactionStatusConstant.FAILED);
        }
        return savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
    }
}
