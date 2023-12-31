package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.SavingsFundingRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.FundSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.PublishTransactionNotificationUseCase;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalFundingFailureEvent;
import com.mintfintech.savingsms.usecase.data.request.SavingFundingRequest;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.referral_savings.CreateReferralRewardUseCase;
import com.mintfintech.savingsms.usecase.features.savings_funding.SavingsFundingUtil;
import com.mintfintech.savingsms.utils.MoneyFormatterUtil;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@FieldDefaults(makeFinal = true)
@Slf4j
@Named
@AllArgsConstructor
public class FundSavingsGoalUseCaseImpl implements FundSavingsGoalUseCase {

    private CoreBankingServiceClient coreBankingServiceClient;
    private SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    private SavingsPlanEntityDao savingsPlanEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private MintAccountEntityDao mintAccountEntityDao;
    private MintBankAccountEntityDao mintBankAccountEntityDao;
    private ApplicationEventService applicationEventService;
    private UpdateBankAccountBalanceUseCase updateAccountBalanceUseCase;
    private AppUserEntityDao appUserEntityDao;
    private TierLevelEntityDao tierLevelEntityDao;
    private SystemIssueLogService systemIssueLogService;
    private PublishTransactionNotificationUseCase publishTransactionNotificationUseCase;
    private CreateReferralRewardUseCase createReferralRewardUseCase;
    private SavingsFundingUtil savingsFundingUtil;

    @Override
    public SavingsGoalFundingResponse fundSavingGoal(AuthenticatedUser authenticatedUser, SavingFundingRequest fundingRequest) {
        AppUserEntity appUserEntity = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        SavingsGoalEntity savingsGoalEntity = savingsGoalEntityDao.findSavingGoalByAccountAndGoalId(accountEntity, fundingRequest.getGoalId())
                .orElseThrow(() -> new BadRequestException("Invalid savings goal Id."));

        if(savingsGoalEntity.getCreationSource() == SavingsGoalCreationSourceConstant.MINT){
            throw new BusinessLogicConflictException("Sorry, this goal cannot be funded because it's created by the system.");
        }
        if(savingsGoalEntity.getGoalStatus() == SavingsGoalStatusConstant.COMPLETED || savingsGoalEntity.getGoalStatus() == SavingsGoalStatusConstant.WITHDRAWN) {
            throw new BusinessLogicConflictException("Funding is no longer supported on this savings goal.");
        }

        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(fundingRequest.getDebitAccountId(), accountEntity)
                .orElseThrow(() -> new BadRequestException("Invalid debit account Id."));

        BigDecimal amount = BigDecimal.valueOf(fundingRequest.getAmount());
       /*
        No maximum amount for savings goal
        SavingsPlanEntity planEntity = savingsPlanEntityDao.getRecordById(savingsGoalEntity.getSavingsPlan().getId());
        if(planEntity.getPlanName() != SavingsPlanTypeConstant.SAVINGS_TIER_THREE) {
            BigDecimal currentBalance = savingsGoalEntity.getSavingsBalance();
            BigDecimal totalAmount = currentBalance.add(amount);
            if(totalAmount.compareTo(planEntity.getMaximumBalance()) > 0) {
                throw new BusinessLogicConflictException("Sorry, maximum amount for your savings plan is N"+ MoneyFormatterUtil.priceWithDecimal(planEntity.getMaximumBalance()));
            }
        }*/
        if(debitAccount.getAvailableBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Sorry, you do not have sufficient balance in your account for this request.");
        }
        TierLevelEntity tierLevelEntity = tierLevelEntityDao.getRecordById(debitAccount.getAccountTierLevel().getId());
        if(tierLevelEntity.getLevel() != TierLevelTypeConstant.TIER_THREE) {
            if(amount.compareTo(tierLevelEntity.getBulletTransactionAmount()) > 0) {
                throw new BadRequestException("Sorry, transaction limit on your account tier is N"+MoneyFormatterUtil.priceWithDecimal(tierLevelEntity.getBulletTransactionAmount()));
            }
        }
        SavingsGoalFundingResponse response = fundSavingGoal(debitAccount, appUserEntity, savingsGoalEntity, amount);
        return response;
    }

    @Override
    public void processSavingsGoalScheduledSaving() {
        LocalDateTime now = LocalDateTime.now();
        List<SavingsGoalEntity> savingsGoalEntityList = savingsGoalEntityDao.getSavingGoalWithAutoSaveTime(now);
        for(SavingsGoalEntity savingsGoalEntity: savingsGoalEntityList) {
            if(savingsGoalEntity.getGoalStatus() != SavingsGoalStatusConstant.ACTIVE || !savingsGoalEntity.isAutoSave()) {
                log.info("Savings goal auto funding skipped: {}", savingsGoalEntity.getGoalId());
                continue;
            }
            LocalDateTime adjustedTime = now.withNano(0).withSecond(0).withMinute(0);
            LocalDateTime nextSavingsDate = savingsGoalEntity.getNextAutoSaveDate().withNano(0).withSecond(0).withMinute(0);
            if(!adjustedTime.equals(nextSavingsDate)) {
                log.info("Next saving date does not match:{} - {} - {}", savingsGoalEntity.getGoalId(), adjustedTime.toString(), nextSavingsDate.toString());
                continue;
            }
            LocalDateTime newNextSavingsDate = getNewNextSavingsDate(savingsGoalEntity, nextSavingsDate);
            MintBankAccountEntity debitAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(savingsGoalEntity.getMintAccount(), BankAccountTypeConstant.CURRENT);
            debitAccount = updateAccountBalanceUseCase.processBalanceUpdate(debitAccount);
            BigDecimal savingsAmount = savingsGoalEntity.getSavingsAmount();
            if(debitAccount.getAvailableBalance().compareTo(savingsAmount) < 0) {
                log.info("Insufficient balance for savings auto debit: {}" , savingsGoalEntity.getGoalId());
                savingsGoalEntity.setNextAutoSaveDate(newNextSavingsDate);
                savingsGoalEntityDao.saveRecord(savingsGoalEntity);
                publishTransactionNotificationUseCase.sendSavingsFundingFailureNotification(savingsGoalEntity, savingsAmount, "Insufficient balance to fund savings goal.");
                continue;
            }
            savingsGoalEntity.setNextAutoSaveDate(newNextSavingsDate);
            savingsGoalEntityDao.saveRecord(savingsGoalEntity);
            boolean proceedWithFunding = validateSavingTierRestriction(savingsGoalEntity, savingsAmount);
            if(!proceedWithFunding) {
                continue;
            }
            SavingsGoalFundingResponse fundingResponse = fundSavingGoal(debitAccount, null, savingsGoalEntity, savingsAmount);
            if(!"00".equalsIgnoreCase(fundingResponse.getResponseCode())) {
                publishTransactionNotificationUseCase.sendSavingsFundingFailureNotification(savingsGoalEntity, savingsAmount, fundingResponse.getResponseMessage());

                String message = String.format("Goal Id: %s; message: %s", savingsGoalEntity.getGoalId(),  fundingResponse.getResponseMessage());
                systemIssueLogService.logIssue("Savings Auto-Funding Failed", "Savings funding failure", message);
            }
        }
    }

    private LocalDateTime getNewNextSavingsDate(SavingsGoalEntity savingsGoalEntity, LocalDateTime nextSavingsDate) {
        LocalDateTime newNextSavingsDate = nextSavingsDate.plusDays(1);
        SavingsFrequencyTypeConstant frequencyType = savingsGoalEntity.getSavingsFrequency();
        if (frequencyType == SavingsFrequencyTypeConstant.WEEKLY) {
            newNextSavingsDate = nextSavingsDate.plusWeeks(1);
        } else if (frequencyType == SavingsFrequencyTypeConstant.MONTHLY) {
            newNextSavingsDate = nextSavingsDate.plusMonths(1);
        }
        return newNextSavingsDate;
    }


    private boolean validateSavingTierRestriction(SavingsGoalEntity goalEntity, BigDecimal savingsAmount) {
        SavingsPlanEntity savingsPlanEntity = savingsPlanEntityDao.getRecordById(goalEntity.getSavingsPlan().getId());
        if(savingsPlanEntity.getPlanName() != SavingsPlanTypeConstant.SAVINGS_TIER_THREE) {
            BigDecimal toBeNewBalance = goalEntity.getSavingsBalance().add(savingsAmount);
            if(toBeNewBalance.compareTo(savingsPlanEntity.getMaximumBalance()) > 0) {
                MintBankAccountEntity bankAccountEntity = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(goalEntity.getMintAccount(), BankAccountTypeConstant.CURRENT);
                if(bankAccountEntity.getAccountTierLevel().getLevel() != TierLevelTypeConstant.TIER_THREE) {
                    log.info("Savings goal: {} Auto debit ignored. New balance {} will be above maximum balance for plan: {}" , goalEntity.getGoalId(), toBeNewBalance, savingsPlanEntity.getMaximumBalance());
                    AppUserEntity appUserEntity = appUserEntityDao.getRecordById(goalEntity.getCreator().getId());
                    SavingsGoalFundingFailureEvent  failureEvent = SavingsGoalFundingFailureEvent.builder()
                            .failureMessage("Maximum balance for savings plan is exceeded. Update savings plan.")
                            .amount(savingsAmount)
                            .status("ABORTED")
                            .name(appUserEntity.getName())
                            .recipient(appUserEntity.getEmail()).build();
                    applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_SAVINGS_GOAL_FUNDING_FAILURE, new EventModel<>(failureEvent));
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public SavingsGoalFundingResponse fundSavingGoal(MintBankAccountEntity debitAccount, AppUserEntity appUserEntity, SavingsGoalEntity savingsGoal, BigDecimal amount) {

        SavingsGoalTransactionEntity transactionEntity = new SavingsGoalTransactionEntity();
        transactionEntity.setSavingsGoal(savingsGoal);
        transactionEntity.setBankAccount(debitAccount);
        transactionEntity.setTransactionAmount(amount);
        transactionEntity.setFundingSource(FundingSourceTypeConstant.MINT_ACCOUNT);
        transactionEntity.setCurrentBalance(savingsGoal.getSavingsBalance());
        transactionEntity.setTransactionType(TransactionTypeConstant.CREDIT);
        transactionEntity.setTransactionStatus(TransactionStatusConstant.PENDING);
        transactionEntity.setTransactionReference(savingsGoalTransactionEntityDao.generateTransactionReference());
        transactionEntity.setPerformedBy(appUserEntity);

        transactionEntity = savingsGoalTransactionEntityDao.saveRecord(transactionEntity);

        BigDecimal balanceBeforeTransaction = debitAccount.getAvailableBalance();
        String narration = savingsFundingUtil.constructFundingNarration(savingsGoal);

        SavingsFundingRequestCBS fundingRequestCBS = SavingsFundingRequestCBS.builder()
                .amount(amount)
                .debitAccountNumber(debitAccount.getAccountNumber())
                .goalId(savingsGoal.getGoalId())
                .goalName(savingsGoal.getName())
                .narration(narration)
                .reference(transactionEntity.getTransactionReference())
                .build();
        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processSavingFunding(fundingRequestCBS);
        transactionEntity = savingsFundingUtil.processFundingTransactionResponse(transactionEntity, msClientResponse);

        SavingsGoalFundingResponse fundingResponse = SavingsGoalFundingResponse.builder()
                .responseCode(transactionEntity.getTransactionResponseCode())
                .responseMessage(transactionEntity.getTransactionResponseMessage())
                .transactionReference(transactionEntity.getTransactionReference())
                .build();
        if(transactionEntity.getTransactionStatus() == TransactionStatusConstant.SUCCESSFUL) {
            savingsGoal.setSavingsBalance(savingsGoal.getSavingsBalance().add(transactionEntity.getTransactionAmount()));
            if(savingsGoal.getGoalStatus() == SavingsGoalStatusConstant.INACTIVE) {
                savingsGoal.setGoalStatus(SavingsGoalStatusConstant.ACTIVE);
            }
            savingsGoalEntityDao.saveRecord(savingsGoal);
            transactionEntity.setNewBalance(savingsGoal.getSavingsBalance());
            savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
            debitAccount = updateAccountBalanceUseCase.processBalanceUpdate(debitAccount);
            BigDecimal newBalance = debitAccount.getAvailableBalance();
            publishTransactionNotificationUseCase.createTransactionLog(transactionEntity, balanceBeforeTransaction, newBalance);
            publishTransactionNotificationUseCase.sendSavingsFundingSuccessNotification(transactionEntity);
            fundingResponse.setResponseCode("00");
            fundingResponse.setResponseMessage("Transaction processed successfully.");
           // createReferralRewardUseCase.processReferredCustomerReward(debitAccount.getMintAccount(), savingsGoal);
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
