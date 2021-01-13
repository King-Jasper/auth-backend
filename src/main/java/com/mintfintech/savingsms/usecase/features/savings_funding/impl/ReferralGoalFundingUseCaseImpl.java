package com.mintfintech.savingsms.usecase.features.savings_funding.impl;

import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.ReferralSavingsFundingRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;
import com.mintfintech.savingsms.usecase.features.savings_funding.ReferralGoalFundingUseCase;
import com.mintfintech.savingsms.usecase.features.savings_funding.SavingsFundingUtil;
import lombok.AllArgsConstructor;
import javax.inject.Named;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Wed, 13 Jan, 2021
 */
@Named
@AllArgsConstructor
public class ReferralGoalFundingUseCaseImpl implements ReferralGoalFundingUseCase {

    private final SavingsGoalEntityDao savingsGoalEntityDao;
    private final CoreBankingServiceClient coreBankingServiceClient;
    private final SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final SavingsFundingUtil savingsFundingUtil;

    @Override
    public SavingsGoalFundingResponse fundReferralSavingsGoal(SavingsGoalEntity savingsGoal, BigDecimal amount) {

        SavingsGoalTransactionEntity transactionEntity = SavingsGoalTransactionEntity.builder()
                .transactionAmount(amount)
                .transactionReference(savingsGoalTransactionEntityDao.generateTransactionReference())
                .bankAccount(mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(savingsGoal.getMintAccount(), BankAccountTypeConstant.CURRENT))
                .transactionType(TransactionTypeConstant.DEBIT)
                .transactionStatus(TransactionStatusConstant.PENDING)
                .fundingSource(FundingSourceTypeConstant.MINT_ACCOUNT)
                .savingsGoal(savingsGoal)
                .currentBalance(savingsGoal.getSavingsBalance())
                .build();
        transactionEntity = savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
        transactionEntity.setRecordStatus(RecordStatusConstant.INACTIVE);

        String narration = savingsFundingUtil.constructFundingNarration(savingsGoal);
        ReferralSavingsFundingRequestCBS fundingRequestCBS = ReferralSavingsFundingRequestCBS.builder()
                .amount(amount)
                .goalId(savingsGoal.getGoalId())
                .goalName(savingsGoal.getName())
                .reference(transactionEntity.getTransactionReference())
                .narration(narration)
                .build();
        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processSavingReferralFunding(fundingRequestCBS);
        transactionEntity = savingsFundingUtil.processFundingTransactionResponse(transactionEntity, msClientResponse);

        SavingsGoalFundingResponse fundingResponse = SavingsGoalFundingResponse.builder()
                .responseCode(transactionEntity.getTransactionResponseCode())
                .responseMessage(transactionEntity.getTransactionResponseMessage())
                .transactionReference(transactionEntity.getTransactionReference())
                .build();

        if(transactionEntity.getTransactionStatus() == TransactionStatusConstant.SUCCESSFUL) {
            savingsGoal.setSavingsBalance(savingsGoal.getSavingsBalance().add(amount));
            savingsGoalEntityDao.saveRecord(savingsGoal);
            if(savingsGoal.getGoalStatus() == SavingsGoalStatusConstant.INACTIVE) {
                savingsGoal.setGoalStatus(SavingsGoalStatusConstant.ACTIVE);
                savingsGoalEntityDao.saveRecord(savingsGoal);
            }
            transactionEntity.setNewBalance(savingsGoal.getSavingsBalance());
            savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
            // publishTransactionNotificationUseCase.sendSavingsFundingSuccessNotification(transactionEntity);
            fundingResponse.setResponseCode("00");
        }else if(transactionEntity.getTransactionStatus() == TransactionStatusConstant.PENDING) {
            fundingResponse.setResponseCode("01");
            fundingResponse.setResponseMessage("Transaction status pending. Please check your balance before trying again.");
        }else {
            fundingResponse.setResponseCode("02");
        }
        fundingResponse.setSavingsBalance(savingsGoal.getSavingsBalance());
        return fundingResponse;
    }
}
