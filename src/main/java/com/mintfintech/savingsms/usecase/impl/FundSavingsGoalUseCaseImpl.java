package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.MintFundTransferRequestCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.SavingsFundingRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.FundSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.MintTransactionEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.PushNotificationEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalFundingEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalFundingFailureEvent;
import com.mintfintech.savingsms.usecase.data.request.SavingFundingRequest;
import com.mintfintech.savingsms.usecase.data.response.SavingsGoalFundingResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.utils.MoneyFormatterUtil;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Override
    public SavingsGoalFundingResponse fundSavingGoal(AuthenticatedUser authenticatedUser, SavingFundingRequest fundingRequest) {
        AppUserEntity appUserEntity = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        SavingsGoalEntity savingsGoalEntity = savingsGoalEntityDao.findSavingGoalByAccountAndGoalId(accountEntity, fundingRequest.getGoalId())
                .orElseThrow(() -> new BadRequestException("Invalid savings goal Id."));

        if(savingsGoalEntity.getCreationSource() == SavingsGoalCreationSourceConstant.MINT){
            throw new BusinessLogicConflictException("Sorry, this goal cannot be funded because it's created by the system.");
        }

        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(fundingRequest.getDebitAccountId(), accountEntity)
                .orElseThrow(() -> new BadRequestException("Invalid debit account Id."));

        BigDecimal amount = BigDecimal.valueOf(fundingRequest.getAmount());
        SavingsPlanEntity planEntity = savingsPlanEntityDao.getRecordById(savingsGoalEntity.getSavingsPlan().getId());

        if(planEntity.getPlanName() != SavingsPlanTypeConstant.SAVINGS_TIER_THREE) {
            BigDecimal currentBalance = savingsGoalEntity.getSavingsBalance();
            BigDecimal totalAmount = currentBalance.add(amount);
            if(totalAmount.compareTo(planEntity.getMaximumBalance()) > 0) {
                throw new BusinessLogicConflictException("Sorry, maximum amount for your savings plan is N"+ MoneyFormatterUtil.priceWithDecimal(planEntity.getMaximumBalance()));
            }
        }
        if(debitAccount.getAvailableBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Sorry, you have sufficient balance in your account for this request.");
        }
        TierLevelEntity tierLevelEntity = tierLevelEntityDao.getRecordById(debitAccount.getAccountTierLevel().getId());
        if(tierLevelEntity.getLevel() != TierLevelTypeConstant.TIER_THREE) {
            if(amount.compareTo(tierLevelEntity.getBulletTransactionAmount()) > 0) {
                throw new BadRequestException("Sorry, transaction limit on your account tier is N"+MoneyFormatterUtil.priceWithDecimal(tierLevelEntity.getBulletTransactionAmount()));
            }
        }
        SavingsGoalFundingResponse response = fundSavingGoal(debitAccount, appUserEntity, savingsGoalEntity, amount);
        if("00".equalsIgnoreCase(response.getResponseCode())){
             sendSavingsFundingSuccessNotification(savingsGoalEntity, response, amount);
        }
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
            LocalDateTime newNextSavingsDate = nextSavingsDate.plusDays(1);
            SavingsFrequencyTypeConstant frequencyType = savingsGoalEntity.getSavingsFrequency();
            if(frequencyType == SavingsFrequencyTypeConstant.WEEKLY){
                newNextSavingsDate = nextSavingsDate.plusWeeks(1);
            }else if(frequencyType == SavingsFrequencyTypeConstant.MONTHLY) {
                newNextSavingsDate = nextSavingsDate.plusMonths(1);
            }
            MintBankAccountEntity debitAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(savingsGoalEntity.getMintAccount(), BankAccountTypeConstant.CURRENT);
            debitAccount = updateAccountBalanceUseCase.processBalanceUpdate(debitAccount);
            BigDecimal savingsAmount = savingsGoalEntity.getSavingsAmount();
            if(debitAccount.getAvailableBalance().compareTo(savingsAmount) < 0) {
                log.info("Insufficient balance for savings auto debit: {}" , savingsGoalEntity.getGoalId());
                savingsGoalEntity.setNextAutoSaveDate(newNextSavingsDate);
                savingsGoalEntityDao.saveRecord(savingsGoalEntity);
                sendSavingsFundingFailureNotification(savingsGoalEntity, savingsAmount, "Insufficient balance to fund savings goal.");
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
                sendSavingsFundingFailureNotification(savingsGoalEntity, savingsAmount, fundingResponse.getResponseMessage());
            }
            if("00".equalsIgnoreCase(fundingResponse.getResponseCode())) {
                sendSavingsFundingSuccessNotification(savingsGoalEntity, fundingResponse, savingsAmount);
            }
        }
    }

    private void sendSavingsFundingFailureNotification(SavingsGoalEntity goalEntity, BigDecimal savingsAmount, String failureMessage) {
        AppUserEntity appUserEntity = appUserEntityDao.getRecordById(goalEntity.getCreator().getId());
        if(!appUserEntity.isEmailNotificationEnabled()){
            log.info("Email notification disabled: {}", appUserEntity.getEmail());
            return;
        }
        SavingsGoalFundingFailureEvent  failureEvent = SavingsGoalFundingFailureEvent.builder()
                .failureMessage(failureMessage)
                .amount(savingsAmount)
                .goalName(goalEntity.getName())
                .status("FAILED")
                .name(appUserEntity.getName())
                .transactionDate(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .recipient(appUserEntity.getEmail()).build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_SAVINGS_GOAL_FUNDING_FAILURE, new EventModel<>(failureEvent));
    }

    private void sendSavingsFundingSuccessNotification(SavingsGoalEntity goalEntity, SavingsGoalFundingResponse fundingResponse, BigDecimal savingsAmount) {
        AppUserEntity appUserEntity = appUserEntityDao.getRecordById(goalEntity.getCreator().getId());
        if(appUserEntity.isEmailNotificationEnabled()){
            SavingsGoalFundingEvent fundingEvent = SavingsGoalFundingEvent.builder()
                    .amount(savingsAmount)
                    .goalName(goalEntity.getName())
                    .reference(fundingResponse.getTransactionReference())
                    .name(appUserEntity.getName())
                    .recipient(appUserEntity.getEmail())
                    .transactionDate(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                    .build();
            applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_SAVINGS_GOAL_FUNDING_SUCCESS, new EventModel<>(fundingEvent));
        }else {
            log.info("Email notification disabled: {}", appUserEntity.getEmail());
        }
        if(appUserEntity.isGcmNotificationEnabled()) {
            if("".equalsIgnoreCase(appUserEntity.getDeviceGcmNotificationToken())) {
                //device token does not exist for user. probably a web user.
                return;
            }
            String text = String.format("Congrats, you just saved N%s in your savings goal(%s)", MoneyFormatterUtil.priceWithoutDecimal(savingsAmount), goalEntity.getName());
            PushNotificationEvent pushNotificationEvent = new PushNotificationEvent("New Savings", text, appUserEntity.getDeviceGcmNotificationToken());
            pushNotificationEvent.setUserId(appUserEntity.getUserId());
            if(appUserEntity.getDeviceGcmNotificationToken() == null) {
                //accounts will publish the device token value if it exist.
                applicationEventService.publishEvent(ApplicationEventService.EventType.PUSH_NOTIFICATION_TOKEN_ACCOUNTS, new EventModel<>(pushNotificationEvent));
                appUserEntity.setDeviceGcmNotificationToken("");
                appUserEntityDao.saveRecord(appUserEntity);
            }else {
                applicationEventService.publishEvent(ApplicationEventService.EventType.PUSH_NOTIFICATION_TOKEN, new EventModel<>(pushNotificationEvent));
            }
        }
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

        SavingsGoalTransactionEntity transactionEntity = SavingsGoalTransactionEntity.builder()
                .transactionAmount(amount)
                .transactionReference(savingsGoalTransactionEntityDao.generateTransactionReference())
                .bankAccount(debitAccount)
                .transactionType(TransactionTypeConstant.CREDIT)
                .transactionStatus(TransactionStatusConstant.PENDING)
                .savingsGoal(savingsGoal)
                .performedBy(appUserEntity)
                .currentBalance(savingsGoal.getSavingsBalance())
                .build();
        transactionEntity = savingsGoalTransactionEntityDao.saveRecord(transactionEntity);

        BigDecimal balanceBeforeTransaction = debitAccount.getAvailableBalance();
        String narration = constructFundingNarration(savingsGoal);

        SavingsFundingRequestCBS fundingRequestCBS = SavingsFundingRequestCBS.builder()
                .amount(amount)
                .debitAccountNumber(debitAccount.getAccountNumber())
                .goalId(savingsGoal.getGoalId())
                .goalName(savingsGoal.getName())
                .narration(narration)
                .reference(transactionEntity.getTransactionReference())
                .build();
        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processSavingFunding(fundingRequestCBS);
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
        SavingsGoalEntity savingsGoalEntity = savingsGoalEntityDao.getRecordById(savingsGoalTransactionEntity.getSavingsGoal().getId());
        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.getRecordById(savingsGoalTransactionEntity.getBankAccount().getId());
        String description = "Savings Goal funding - "+savingsGoalEntity.getGoalId()+"|"+savingsGoalEntity.getName();
        MintTransactionEvent transactionPayload = MintTransactionEvent.builder()
                .balanceAfterTransaction(currentBalance)
                .balanceBeforeTransaction(openingBalance)
                .transactionAmount(savingsGoalTransactionEntity.getTransactionAmount())
                .transactionType(TransactionTypeConstant.DEBIT.name())
                .category("SAVINGS_GOAL")
                .debitAccountId(debitAccount.getAccountId())
                .description(description)
                .externalReference(savingsGoalTransactionEntity.getExternalReference())
                .internalReference(savingsGoalTransactionEntity.getTransactionReference())
                .spendingTagId(0)
                .dateCreated(savingsGoalTransactionEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.MINT_TRANSACTION_LOG, new EventModel<>(transactionPayload));
    }

    private String constructFundingNarration(SavingsGoalEntity savingsGoalEntity) {
        String narration = String.format("SGF-%s %s", savingsGoalEntity.getGoalId(), savingsGoalEntity.getName());
        if(narration.length() > 61) {
            return narration.substring(0, 60);
        }
        return narration;
    }
}
