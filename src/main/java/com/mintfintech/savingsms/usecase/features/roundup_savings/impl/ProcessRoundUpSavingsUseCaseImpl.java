package com.mintfintech.savingsms.usecase.features.roundup_savings.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.SavingsFundingRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.usecase.PublishTransactionNotificationUseCase;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.incoming.MintTransactionPayload;
import com.mintfintech.savingsms.usecase.data.value_objects.RoundUpTransactionCategoryType;
import com.mintfintech.savingsms.usecase.features.roundup_savings.ProcessRoundUpSavingsUseCase;
import com.mintfintech.savingsms.usecase.features.savings_funding.SavingsFundingUtil;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private SpendAndSaveEntityDao spendAndSaveEntityDao;
    private SpendAndSaveTransactionDao spendAndSaveTransactionDao;

    @Override
    public void processTransactionForRoundUpSavings(MintTransactionPayload transactionPayload) {
        String reference = transactionPayload.getInternalReference();
        String category = transactionPayload.getCategory();
        String type = transactionPayload.getTransactionType();
        if (roundUpSavingsTransactionEntityDao.findByTransactionReference(reference).isPresent()) {
            return;
        }
        RoundUpTransactionCategoryType transactionCategory = RoundUpTransactionCategoryType.getByName(category);
        if (!"DEBIT".equalsIgnoreCase(type) || transactionCategory == null) {
            return;
        }
        Optional<MintBankAccountEntity> debitAccountOpt = mintBankAccountEntityDao.findByAccountId(transactionPayload.getDebitAccountId());
        if (!debitAccountOpt.isPresent()) {
            return;
        }
        MintBankAccountEntity debitAccount = debitAccountOpt.get();
        Optional<RoundUpSavingsSettingEntity> settingEntityOpt = roundUpSavingsSettingEntityDao.findRoundUpSavingsByAccount(debitAccount.getMintAccount());
        if (!settingEntityOpt.isPresent() || !settingEntityOpt.get().isEnabled()) {
            return;
        }
        RoundUpSavingsSettingEntity settingEntity = settingEntityOpt.get();
        RoundUpSavingsTypeConstant roundUpType = getRoundUpType(transactionCategory, settingEntity);
        if (roundUpType == RoundUpSavingsTypeConstant.NONE) {
            return;
        }
        BigDecimal transactionAmount = transactionPayload.getTransactionAmount();
        BigDecimal amountToSave = getAmountToSave(roundUpType, transactionAmount);
        if (amountToSave.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Amount to save is {}", amountToSave);
            return;
        }
        if (!hasSufficientBalance(transactionPayload, amountToSave)) {
            log.info("Insufficient balance to process roundup savings");
            return;
        }
        MintAccountEntity accountEntity = debitAccount.getMintAccount();
        Optional<SavingsGoalEntity> roundUpSavingsOpt = savingsGoalEntityDao.findFirstSavingsByType(accountEntity, SavingsGoalTypeConstant.ROUND_UP_SAVINGS);
        if (!roundUpSavingsOpt.isPresent()) {
            return;
        }
        SavingsGoalEntity roundUpSavings = roundUpSavingsOpt.get();
        if(roundUpSavings.getGoalStatus() != SavingsGoalStatusConstant.ACTIVE) {
            log.info("savings goal is no longer active");
            return;
        }
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
        //processSavingFunding(savingsTransactionEntity);
    }

    @Override
    public void processTransactionForSpendAndSave(MintTransactionPayload transactionPayload) {

        String reference = transactionPayload.getInternalReference();
        String category = transactionPayload.getCategory();

        if (spendAndSaveTransactionDao.findByTransactionReference(reference).isPresent()) {
            return;
        }
        RoundUpTransactionCategoryType transactionCategory = RoundUpTransactionCategoryType.getByName(category);
        System.out.println("RoundUpTransactionCategoryType - "+transactionCategory+" "+transactionPayload.getDebitAccountId());
        if (transactionCategory == null || transactionCategory.equals(RoundUpTransactionCategoryType.CARD_PAYMENT)) {
            return;
        }
        Optional<MintBankAccountEntity> debitAccountOpt = mintBankAccountEntityDao.findByAccountId(transactionPayload.getDebitAccountId());
        if (!debitAccountOpt.isPresent()) {
            return;
        }
        MintBankAccountEntity debitAccount = debitAccountOpt.get();
        Optional<SpendAndSaveEntity> settingEntityOptional = spendAndSaveEntityDao.findSpendAndSaveSettingByAccount(debitAccount.getMintAccount());
        if (!settingEntityOptional.isPresent() || !settingEntityOptional.get().isActivated()) {
            return;
        }

        SpendAndSaveEntity settingEntity = settingEntityOptional.get();
        BigDecimal transactionAmount = transactionPayload.getTransactionAmount();
        BigDecimal amountToSave = getSaveAmount(settingEntity.getPercentage(), transactionAmount);
        if (amountToSave.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Amount to save is {}", amountToSave);
            return;
        }
        if (!hasSufficientBalance(transactionPayload, amountToSave)) {
            log.info("Insufficient balance to process roundup savings");
            return;
        }
        MintAccountEntity accountEntity = debitAccount.getMintAccount();
        Optional<SavingsGoalEntity> savingsGoalOptional = savingsGoalEntityDao.findFirstSavingsByType(accountEntity, SavingsGoalTypeConstant.SPEND_AND_SAVE);
        if (!savingsGoalOptional.isPresent()) {
            return;
        }
        SavingsGoalEntity savingsGoal = savingsGoalOptional.get();
        if(savingsGoal.getGoalStatus() != SavingsGoalStatusConstant.ACTIVE) {
            log.info("savings goal is no longer active");
            return;
        }

        processSavingFunding(settingEntity, savingsGoal, transactionPayload, amountToSave);
    }

    private BigDecimal getSaveAmount(int percentage, BigDecimal transactionAmount) {
        BigDecimal percent = BigDecimal.valueOf(percentage);
        return (percent.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN)).multiply(transactionAmount);
    }


    private RoundUpSavingsTypeConstant getRoundUpType(RoundUpTransactionCategoryType transactionCategory, RoundUpSavingsSettingEntity settingEntity) {
        RoundUpSavingsTypeConstant typeConstant;
        switch (transactionCategory) {
            case FUND_TRANSFER:
                typeConstant = settingEntity.getFundTransferRoundUpType();
                break;
            case BILL_PAYMENT:
                typeConstant = settingEntity.getBillPaymentRoundUpType();
                break;
            case CARD_PAYMENT:
                typeConstant = settingEntity.getCardPaymentRoundUpType();
                break;
            default:
                typeConstant = RoundUpSavingsTypeConstant.NONE;
        }
        return typeConstant;
    }

    private BigDecimal getAmountToSave(RoundUpSavingsTypeConstant roundUpType, BigDecimal transactionAmount) {
        BigDecimal value = BigDecimal.valueOf(10.0);
        if (roundUpType == RoundUpSavingsTypeConstant.NEAREST_HUNDRED) {
            value = BigDecimal.valueOf(100.0);
        } else if (roundUpType == RoundUpSavingsTypeConstant.NEAREST_THOUSAND) {
            value = BigDecimal.valueOf(1000.0);
        }
        BigDecimal remainder = transactionAmount.remainder(value);
        BigDecimal amountToSaved = value.subtract(remainder);
        if (amountToSaved.compareTo(value) == 0) {
            return BigDecimal.ZERO;
        }
        return amountToSaved;
    }

    private boolean hasSufficientBalance(MintTransactionPayload payload, BigDecimal amountToSave) {
        BigDecimal currentBalance = payload.getBalanceAfterTransaction();
        if (currentBalance == null) {
            currentBalance = payload.getBalanceBeforeTransaction().subtract(payload.getTransactionAmount());
        }
        if (currentBalance.compareTo(amountToSave) > 0) {
            return true;
        }
        log.info("Insufficient Balance {}: {}", currentBalance, amountToSave);
        return false;
    }

    private void processSavingFunding(SpendAndSaveEntity spendAndSave, SavingsGoalEntity goalEntity, MintTransactionPayload transactionPayload, BigDecimal amountToSave) {

        Optional<MintBankAccountEntity> debitAccountOpt = mintBankAccountEntityDao.findByAccountId(transactionPayload.getDebitAccountId());
        if (!debitAccountOpt.isPresent()) {
            return;
        }
        MintBankAccountEntity debitAccount = debitAccountOpt.get();
        String reference = savingsGoalTransactionEntityDao.generateTransactionReference();
        BigDecimal savingsNewBalance = goalEntity.getSavingsBalance().add(amountToSave);

        SavingsGoalTransactionEntity transactionEntity = new SavingsGoalTransactionEntity();
        transactionEntity.setTransactionStatus(TransactionStatusConstant.PENDING);
        transactionEntity.setTransactionType(TransactionTypeConstant.CREDIT);
        transactionEntity.setNewBalance(savingsNewBalance);
        transactionEntity.setTransactionAmount(amountToSave);
        transactionEntity.setSavingsGoal(goalEntity);
        transactionEntity.setBankAccount(debitAccount);
        transactionEntity.setFundingSource(FundingSourceTypeConstant.MINT_ACCOUNT);
        transactionEntity.setCurrentBalance(goalEntity.getSavingsBalance());
        transactionEntity.setTransactionReference(reference);
        transactionEntity = savingsGoalTransactionEntityDao.saveRecord(transactionEntity);

        LocalDateTime transactionDate = LocalDateTime.now();
        if(StringUtils.isNotEmpty(transactionPayload.getDateCreated())) {
            LocalDateTime.parse(transactionPayload.getDateCreated(), DateTimeFormatter.ISO_DATE_TIME);
        }
        SpendAndSaveTransactionEntity spendAndSaveTransaction = SpendAndSaveTransactionEntity.builder()
                .transactionAccount(debitAccount)
                .transactionDate(transactionDate)
                .transactionAmount(transactionPayload.getTransactionAmount())
                .transactionType(RoundUpTransactionCategoryType.getByName(transactionPayload.getCategory()))
                .transactionReference(transactionPayload.getInternalReference())
                .amountSaved(amountToSave)
                .savingsGoal(goalEntity)
                .spendAndSaveSetting(spendAndSave)
                .savingsGoalTransaction(transactionEntity)
                .build();
        spendAndSaveTransactionDao.saveRecord(spendAndSaveTransaction);


        SavingsFundingRequestCBS fundingRequestCBS = SavingsFundingRequestCBS.builder()
                .amount(amountToSave)
                .debitAccountNumber(debitAccount.getAccountNumber())
                .goalId(goalEntity.getGoalId())
                .goalName(goalEntity.getName())
                .narration(savingsFundingUtil.constructFundingNarration(goalEntity))
                .reference(reference)
                .build();
        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processSavingFunding(fundingRequestCBS);
        transactionEntity = savingsFundingUtil.processFundingTransactionResponse(transactionEntity, msClientResponse);
        if (transactionEntity.getTransactionStatus() == TransactionStatusConstant.SUCCESSFUL) {
            goalEntity = savingsGoalEntityDao.getRecordById(goalEntity.getId());
            goalEntity.setSavingsBalance(goalEntity.getSavingsBalance().add(amountToSave));
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
