package com.mintfintech.savingsms.usecase.features.roundup_savings.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.SavingsFundingRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.usecase.FundSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.features.roundup_savings.ProcessRoundUpSavingsUseCase;
import com.mintfintech.savingsms.usecase.PublishTransactionNotificationUseCase;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.incoming.MintTransactionPayload;
import com.mintfintech.savingsms.usecase.data.value_objects.RoundUpTransactionCategoryType;
import com.mintfintech.savingsms.usecase.features.savings_funding.SavingsFundingUtil;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
@Slf4j
@FieldDefaults(makeFinal = true)
@Named
@AllArgsConstructor
public class ProcessRoundUpSavingsUseCaseImpl implements ProcessRoundUpSavingsUseCase {

    private RoundUpSavingsSettingEntityDao roundUpSavingsSettingEntityDao;
    private RoundUpSavingsTransactionEntityDao roundUpSavingsTransactionEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    private MintBankAccountEntityDao mintBankAccountEntityDao;
    private CoreBankingServiceClient coreBankingServiceClient;
    private UpdateBankAccountBalanceUseCase updateBankAccountBalanceUseCase;
    private PublishTransactionNotificationUseCase publishTransactionNotificationUseCase;
    private SavingsFundingUtil savingsFundingUtil;

    @Override
    public void processTransactionForRoundUpSavings(MintTransactionPayload transactionPayload) {
        String reference = transactionPayload.getInternalReference();
        String category = transactionPayload.getCategory();
        String type = transactionPayload.getTransactionType();
        if(roundUpSavingsTransactionEntityDao.findByTransactionReference(reference).isPresent()) {
            return;
        }
        RoundUpTransactionCategoryType transactionCategory = RoundUpTransactionCategoryType.getByName(category);
        if(!"DEBIT".equalsIgnoreCase(type) || transactionCategory == null) {
           return;
        }
        Optional<MintBankAccountEntity> debitAccountOpt = mintBankAccountEntityDao.findByAccountId(transactionPayload.getDebitAccountId());
        if(!debitAccountOpt.isPresent()){
            return;
        }
        MintBankAccountEntity debitAccount = debitAccountOpt.get();
        Optional<RoundUpSavingsSettingEntity> settingEntityOpt = roundUpSavingsSettingEntityDao.findRoundUpSavingsByAccount(debitAccount.getMintAccount());
        if(!settingEntityOpt.isPresent() || !settingEntityOpt.get().isEnabled()) {
            return;
        }
        RoundUpSavingsSettingEntity settingEntity = settingEntityOpt.get();
        RoundUpSavingsTypeConstant roundUpType = getRoundUpType(transactionCategory, settingEntity);
        if(roundUpType == RoundUpSavingsTypeConstant.NONE){
            return;
        }
        BigDecimal transactionAmount = transactionPayload.getTransactionAmount();
        BigDecimal amountToSave = getAmountToSave(roundUpType, transactionAmount);
        if(amountToSave.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Amount to save is {}", amountToSave);
            return;
        }
        if(!hasSufficientBalance(transactionPayload, amountToSave)) {
            log.info("Insufficient balance to process roundup savings");
            return;
        }
        MintAccountEntity accountEntity = debitAccount.getMintAccount();
        Optional<SavingsGoalEntity> roundUpSavingsOpt = savingsGoalEntityDao.findFirstSavingsByType(accountEntity, SavingsGoalTypeConstant.ROUND_UP_SAVINGS);
        if(!roundUpSavingsOpt.isPresent()) {
            return;
        }
        SavingsGoalEntity roundUpSavings = roundUpSavingsOpt.get();
        RoundUpSavingsTransactionEntity savingsTransactionEntity = RoundUpSavingsTransactionEntity.builder()
                .savingsGoal(roundUpSavings)
                .savingsRoundUpType(roundUpType)
                .transactionAccount(debitAccount)
                .amountSaved(amountToSave)
                .transactionAmount(transactionAmount)
                .transactionReference(transactionPayload.getInternalReference())
                .transactionType(transactionCategory)
                .build();
        savingsTransactionEntity = roundUpSavingsTransactionEntityDao.saveRecord(savingsTransactionEntity);
        processSavingFunding(savingsTransactionEntity);
    }




    private RoundUpSavingsTypeConstant getRoundUpType(RoundUpTransactionCategoryType transactionCategory, RoundUpSavingsSettingEntity settingEntity) {
        RoundUpSavingsTypeConstant typeConstant;
        switch (transactionCategory) {
            case FUND_TRANSFER: typeConstant = settingEntity.getFundTransferRoundUpType(); break;
            case BILL_PAYMENT: typeConstant = settingEntity.getBillPaymentRoundUpType(); break;
            case CARD_PAYMENT: typeConstant = settingEntity.getCardPaymentRoundUpType(); break;
            default: typeConstant = RoundUpSavingsTypeConstant.NONE;
        }
        return typeConstant;
    }

    private BigDecimal getAmountToSave(RoundUpSavingsTypeConstant roundUpType, BigDecimal transactionAmount) {
        BigDecimal value = BigDecimal.valueOf(10.0);
        if(roundUpType == RoundUpSavingsTypeConstant.NEAREST_HUNDRED) {
            value = BigDecimal.valueOf(100.0);
        }else if(roundUpType == RoundUpSavingsTypeConstant.NEAREST_THOUSAND) {
            value = BigDecimal.valueOf(1000.0);
        }
        BigDecimal remainder = transactionAmount.remainder(value);
        BigDecimal amountToSaved = value.subtract(remainder);
        if(amountToSaved.compareTo(value) == 0) {
            return BigDecimal.ZERO;
        }
        return amountToSaved;
    }

    private boolean hasSufficientBalance(MintTransactionPayload payload, BigDecimal amountToSave) {
        BigDecimal currentBalance = payload.getBalanceAfterTransaction();
        if(currentBalance == null) {
            currentBalance = payload.getBalanceBeforeTransaction().subtract(payload.getTransactionAmount());
        }
        if(currentBalance.compareTo(amountToSave) > 0) {
            return true;
        }
        log.info("Insufficient Balance {}: {}", currentBalance, amountToSave);
        return false;
    }

    private void processSavingFunding(RoundUpSavingsTransactionEntity roundUpSavingsTransactionEntity) {
         SavingsGoalEntity goalEntity = roundUpSavingsTransactionEntity.getSavingsGoal();
         MintBankAccountEntity debitAccount = mintBankAccountEntityDao.getRecordById(roundUpSavingsTransactionEntity.getTransactionAccount().getId());
         BigDecimal amount = roundUpSavingsTransactionEntity.getAmountSaved();

         String reference = savingsGoalTransactionEntityDao.generateTransactionReference();

         BigDecimal savingsNewBalance = goalEntity.getSavingsBalance().add(amount);
         SavingsGoalTransactionEntity transactionEntity = SavingsGoalTransactionEntity.builder()
                 .savingsGoal(goalEntity)
                 .transactionType(TransactionTypeConstant.CREDIT)
                 .transactionStatus(TransactionStatusConstant.PENDING)
                 .fundingSource(FundingSourceTypeConstant.MINT_ACCOUNT)
                 .transactionAmount(amount)
                 .bankAccount(debitAccount)
                 .currentBalance(goalEntity.getSavingsBalance())
                 .newBalance(savingsNewBalance)
                 .transactionReference(reference)
                 .build();
         transactionEntity = savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
        roundUpSavingsTransactionEntity.setTransaction(transactionEntity);
        roundUpSavingsTransactionEntityDao.saveRecord(roundUpSavingsTransactionEntity);

        SavingsFundingRequestCBS fundingRequestCBS = SavingsFundingRequestCBS.builder()
                .amount(amount)
                .debitAccountNumber(debitAccount.getAccountNumber())
                .goalId(goalEntity.getGoalId())
                .goalName(goalEntity.getName())
                .narration(savingsFundingUtil.constructFundingNarration(goalEntity))
                .reference(reference)
                .build();
        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processSavingFunding(fundingRequestCBS);
        transactionEntity = savingsFundingUtil.processFundingTransactionResponse(transactionEntity, msClientResponse);
        if(transactionEntity.getTransactionStatus() == TransactionStatusConstant.SUCCESSFUL) {
            goalEntity = savingsGoalEntityDao.getRecordById(goalEntity.getId());
            goalEntity.setSavingsBalance(goalEntity.getSavingsBalance().add(amount));
            savingsGoalEntityDao.saveRecord(goalEntity);
            transactionEntity.setNewBalance(goalEntity.getSavingsBalance());
            savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
            BigDecimal balanceBeforeTransaction = debitAccount.getAvailableBalance();
            debitAccount = updateBankAccountBalanceUseCase.processBalanceUpdate(debitAccount);
            BigDecimal newBalance = debitAccount.getAvailableBalance();
            publishTransactionNotificationUseCase.createTransactionLog(transactionEntity, balanceBeforeTransaction, newBalance);
            publishTransactionNotificationUseCase.sendSavingsFundingSuccessNotification(transactionEntity);
        }
    }
}
