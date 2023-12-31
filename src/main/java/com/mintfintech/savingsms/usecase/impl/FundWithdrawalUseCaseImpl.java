package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.SavingsWithdrawalRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.*;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.ComputeAvailableAmountUseCase;
import com.mintfintech.savingsms.usecase.FundWithdrawalUseCase;
import com.mintfintech.savingsms.usecase.PushNotificationService;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.PushNotificationEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalCreationEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalWithdrawalSuccessEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SmsLogEvent;
import com.mintfintech.savingsms.usecase.data.request.SavingsWithdrawalRequest;
import com.mintfintech.savingsms.usecase.data.value_objects.SavingsWithdrawalType;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.utils.MoneyFormatterUtil;
import com.mintfintech.savingsms.utils.PhoneNumberUtils;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import javax.inject.Named;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Mon, 06 Apr, 2020
 */
@FieldDefaults(makeFinal = true)
@Slf4j
@Named
@AllArgsConstructor
public class FundWithdrawalUseCaseImpl implements FundWithdrawalUseCase {

    private AppUserEntityDao appUserEntityDao;
    private MintAccountEntityDao mintAccountEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private MintBankAccountEntityDao mintBankAccountEntityDao;
    private SavingsWithdrawalRequestEntityDao savingsWithdrawalRequestEntityDao;
    private SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    private UpdateBankAccountBalanceUseCase updateAccountBalanceUseCase;
    private CoreBankingServiceClient coreBankingServiceClient;
    private SystemIssueLogService systemIssueLogService;
    private ApplicationProperty applicationProperty;
    private ApplicationEventService applicationEventService;
    private TierLevelEntityDao tierLevelEntityDao;
    private ComputeAvailableAmountUseCase computeAvailableAmountUseCase;
    private RoundUpSavingsSettingEntityDao roundUpSavingsSettingEntityDao;
    private AuditTrailService auditTrailService;
    private SettingsEntityDao settingsEntityDao;
    private PushNotificationService pushNotificationService;



    @Override
    public BigDecimal unlockSavings(AuthenticatedUser authenticatedUser, String goalId, String phoneNumber) {
        SavingsGoalEntity savingsGoal = savingsGoalEntityDao.findSavingGoalByGoalId(goalId)
                .orElseThrow(() -> new BadRequestException("Invalid goal Id."));
        AppUserEntity appUser = appUserEntityDao.getRecordById(savingsGoal.getCreator().getId());
        phoneNumber = PhoneNumberUtils.toInternationalFormat(phoneNumber);

        if(!appUser.getPhoneNumber().equalsIgnoreCase(phoneNumber)) {
            throw new BadRequestException("Goal Id and phone number does not match.");
        }
        if(!savingsGoal.isLockedSavings()) {
            throw new BadRequestException("Savings goal is not locked.");
        }
        BigDecimal interest = savingsGoal.getAccruedInterest();
        savingsGoal.setAccruedInterest(BigDecimal.valueOf(0.00));
        savingsGoal.setLockedSavings(false);
        savingsGoalEntityDao.saveRecord(savingsGoal);

        auditTrailService.createAuditLog(authenticatedUser, AuditTrailService.AuditType.UPDATE, "Unlocked a locked savings goal(interest lost)", savingsGoal);
        return interest;
    }

    @Override
    public String withdrawalSavings(AuthenticatedUser authenticatedUser, SavingsWithdrawalRequest withdrawalRequest) {

        boolean enabled = Boolean.parseBoolean(settingsEntityDao.getSettings(SettingsNameTypeConstant.SAVINGS_WITHDRAWAL_ENABLED, "true"));
        if(!enabled) {
            throw new BusinessLogicConflictException("Sorry, savings withdrawal is temporarily disabled. Try again in few minutes");
        }
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        AppUserEntity appUserEntity = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());

       // BigDecimal amountRequested = BigDecimal.valueOf(withdrawalRequest.getAmount());
        SavingsGoalEntity savingsGoal = savingsGoalEntityDao.findSavingGoalByAccountAndGoalId(accountEntity, withdrawalRequest.getGoalId())
                .orElseThrow(() -> new BadRequestException("Invalid savings goal Id."));

        MintBankAccountEntity creditAccount;
        if(StringUtils.isEmpty(withdrawalRequest.getCreditAccountId())){
            creditAccount =  mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(accountEntity, BankAccountTypeConstant.CURRENT);
        }else {
            creditAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(withdrawalRequest.getCreditAccountId(), accountEntity)
                    .orElseThrow(() -> new BadRequestException("Invalid credit account Id."));
            if(creditAccount.getAccountType() != BankAccountTypeConstant.CURRENT) {
                throw new BadRequestException("Invalid credit account Id.");
            }
        }
        SavingsGoalTypeConstant goalType = savingsGoal.getSavingsGoalType();
        if(goalType == SavingsGoalTypeConstant.MINT_REFERRAL_EARNINGS || goalType == SavingsGoalTypeConstant.ROUND_UP_SAVINGS) {
            return processMintSavingsWithdrawal(savingsGoal, creditAccount, appUserEntity);
        }
        if(savingsGoal.getGoalStatus() != SavingsGoalStatusConstant.ACTIVE && savingsGoal.getGoalStatus() != SavingsGoalStatusConstant.MATURED) {
            if(savingsGoal.getGoalStatus() == SavingsGoalStatusConstant.COMPLETED) {
                throw new BusinessLogicConflictException("Processing fund withdrawal. Please be patient.");
            }
            throw new BusinessLogicConflictException("Sorry, savings withdrawal not currently supported.");
        }
        return processCustomerSavingWithdrawal(savingsGoal, appUserEntity);
    }

    private String processMintSavingsWithdrawal(SavingsGoalEntity savingsGoal, MintBankAccountEntity creditAccount, AppUserEntity currentUser) {
        if(savingsGoal.getCreationSource() != SavingsGoalCreationSourceConstant.MINT){
            throw new BusinessLogicConflictException("Sorry, fund withdrawal not yet activated");
        }
        BigDecimal accruedInterest = savingsGoal.getAccruedInterest();
        BigDecimal savingsBalance = savingsGoal.getSavingsBalance();
        boolean matured = computeAvailableAmountUseCase.isMaturedSavingsGoal(savingsGoal);
        if(!matured) {
            throw new BusinessLogicConflictException("Sorry, your savings is not yet matured for withdrawal");
        }
        BigDecimal amountForWithdrawal = savingsBalance.add(accruedInterest);
        /*if(amountRequested.compareTo(totalAvailableAmount) > 0) {
            throw new BadRequestException("Amount requested ("+MoneyFormatterUtil.priceWithDecimal(amountRequested)+") cannot be above total available balance ("+MoneyFormatterUtil.priceWithDecimal(totalAvailableAmount)+")");
        }*/
        BigDecimal remainingInterest = BigDecimal.ZERO, remainingSavings = BigDecimal.ZERO;
       /* int outcome = amountRequested.compareTo(savingsBalance);
        if(outcome > 0) {
            remainingInterest = totalAvailableAmount.subtract(amountRequested);
            remainingSavings = BigDecimal.ZERO;
        }else if(outcome == 0) {
            remainingInterest = accruedInterest;
            remainingSavings = BigDecimal.ZERO;
        }else {
            remainingInterest = accruedInterest;
            remainingSavings = savingsBalance.subtract(amountRequested);
        }*/
        savingsGoal.setSavingsBalance(remainingSavings);
        savingsGoal.setAccruedInterest(remainingInterest);
        savingsGoalEntityDao.saveRecord(savingsGoal);

        SavingsWithdrawalRequestEntity withdrawalRequest = SavingsWithdrawalRequestEntity.builder()
                .amount(amountForWithdrawal)
                .savingsBalanceWithdrawal(savingsBalance.subtract(remainingSavings))
                .withdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_INTEREST_CREDIT)
                .interestWithdrawal(accruedInterest.subtract(remainingInterest))
                .balanceBeforeWithdrawal(savingsBalance)
                .maturedGoal(true)
                .savingsGoal(savingsGoal)
                .requestedBy(currentUser)
                .build();
        savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequest);

        if(savingsGoal.getSavingsGoalType() == SavingsGoalTypeConstant.ROUND_UP_SAVINGS) {
            savingsGoal.setGoalStatus(SavingsGoalStatusConstant.COMPLETED);
            savingsGoal.setRecordStatus(RecordStatusConstant.DELETED);
            savingsGoalEntityDao.saveRecord(savingsGoal);

            Optional<RoundUpSavingsSettingEntity> settingOpt = roundUpSavingsSettingEntityDao.findRoundUpSavingsByAccount(savingsGoal.getMintAccount());
            if(settingOpt.isPresent()) {
                RoundUpSavingsSettingEntity settingEntity = settingOpt.get();
                settingEntity.setRecordStatus(RecordStatusConstant.DELETED);
                roundUpSavingsSettingEntityDao.saveRecord(settingEntity);
            }
        }
        if(savingsGoal.getSavingsGoalType() == SavingsGoalTypeConstant.MINT_REFERRAL_EARNINGS) {
            savingsGoal.setGoalStatus(SavingsGoalStatusConstant.COMPLETED);
            savingsGoal.setRecordStatus(RecordStatusConstant.INACTIVE);
            savingsGoalEntityDao.saveRecord(savingsGoal);
        }
        return "Request queued successfully. Your account will be funded very soon.";
    }

    @Transactional
    public String processCustomerSavingWithdrawal(SavingsGoalEntity savingsGoal, AppUserEntity currentUser) {
        LocalDateTime now = LocalDateTime.now();
        boolean isMatured = computeAvailableAmountUseCase.isMaturedSavingsGoal(savingsGoal);
        if(!isMatured) {
            if(savingsGoal.isLockedSavings()) {
                throw new BusinessLogicConflictException("Your savings goal is not yet matured for withdrawal.");
            }
            long numberOfDaysSaved = savingsGoal.getDateCreated().until(now, ChronoUnit.DAYS);
            int minimumDaysForWithdrawal = applicationProperty.savingsMinimumNumberOfDaysForWithdrawal();
            if(numberOfDaysSaved < minimumDaysForWithdrawal) {
                throw new BusinessLogicConflictException("Sorry, you have "+(minimumDaysForWithdrawal - numberOfDaysSaved)+" days left before you can withdraw your savings.");
            }
        }
        createWithdrawalRequest(savingsGoal, isMatured, currentUser);
        /*
        if(isMatured) {
            if(isWithdrawnAtNonProcessingTime()) {
                return "Request queued successfully. Your withdrawal will be processed by 9am.";
            }else {
                return "Request queued successfully. Your account will be funded shortly.";
            }
        }
        return "Request queued successfully. Your account will be funded within the next 2 business days.";
        */
        if(isWithdrawnAtNonProcessingTime()) {
            pushNotificationService.sendMessage(currentUser, "Savings Withdrawal Update",
                    "Hi "+currentUser.getFirstName()+", your account will be credited by 9am. Kindly exercise patient as we process your withdrawal.");
            return "Request queued successfully. Your withdrawal will be processed by 9am.";
        }else {
            return "Request queued successfully. Your account will be funded shortly.";
        }
    }

    private boolean isWithdrawnAtNonProcessingTime() {
         LocalDateTime currentTime = LocalDateTime.now();
         LocalDateTime startTime = LocalDate.now().atTime(LocalTime.of(6, 0));
         LocalDateTime endTime = LocalDate.now().atTime(LocalTime.of(9, 0));
         return currentTime.isAfter(startTime) && currentTime.isBefore(endTime);
    }

    private synchronized void createWithdrawalRequest(SavingsGoalEntity savingsGoal, boolean maturedGoal, AppUserEntity currentUser) {
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusSeconds(120);
        if(savingsWithdrawalRequestEntityDao.countWithdrawalRequestWithinPeriod(savingsGoal, twoMinutesAgo, LocalDateTime.now()) > 0) {
            throw new BusinessLogicConflictException("Possible duplicate withdrawal request.");
        }
        BigDecimal withdrawalAmount = computeAvailableAmountUseCase.getAvailableAmount(savingsGoal);
        LocalDate dateForWithdrawal = LocalDate.now();
        BigDecimal currentBalance = savingsGoal.getSavingsBalance();
        BigDecimal accruedInterest = savingsGoal.getAccruedInterest();
        BigDecimal remainingSavings = BigDecimal.valueOf(0.00), remainingInterest = BigDecimal.valueOf(0.00);
        if(!maturedGoal) {
           // there is deduction on accrued interest for un-matured goal
            remainingInterest = currentBalance.add(accruedInterest).subtract(withdrawalAmount);
            if(remainingInterest.compareTo(BigDecimal.ZERO) < 0) {
                remainingInterest = BigDecimal.ZERO;
            }
           // dateForWithdrawal = DateUtil.addWorkingDays(dateForWithdrawal, 2);
        }
        savingsGoal.setGoalStatus(SavingsGoalStatusConstant.COMPLETED);
        savingsGoal.setAccruedInterest(remainingInterest);
        savingsGoal.setSavingsBalance(remainingSavings);
        savingsGoalEntityDao.saveRecord(savingsGoal);

        BigDecimal interestForWithdrawal = accruedInterest.subtract(remainingInterest);

        BigDecimal withHoldingTax = BigDecimal.ZERO;
        LocalDateTime whtChargeStartDate = LocalDateTime.of(2023, 10, 21, 0, 0, 0);
        if(savingsGoal.getDateCreated().isAfter(whtChargeStartDate) && savingsGoal.getSavingsGoalType() == SavingsGoalTypeConstant.CUSTOMER_SAVINGS) {
            withHoldingTax = interestForWithdrawal.multiply(BigDecimal.valueOf(0.1));
        }

        MintBankAccountEntity creditAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(savingsGoal.getMintAccount(), BankAccountTypeConstant.CURRENT);
        TierLevelEntity tierLevelEntity = tierLevelEntityDao.getRecordById(creditAccount.getAccountTierLevel().getId());
        if(tierLevelEntity.getLevel() == TierLevelTypeConstant.TIER_THREE || (withdrawalAmount.compareTo(tierLevelEntity.getBulletTransactionAmount()) <= 0)) {
            // no need of splitting the withdrawal due to bullet transaction limit.
            SavingsWithdrawalRequestEntity withdrawalRequest = SavingsWithdrawalRequestEntity.builder()
                    .amount(withdrawalAmount)
                    .withdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_INTEREST_CREDIT)
                    .interestWithdrawal(interestForWithdrawal)
                    .savingsBalanceWithdrawal(currentBalance.subtract(remainingSavings))
                    .balanceBeforeWithdrawal(currentBalance)
                    .maturedGoal(maturedGoal)
                    .savingsGoal(savingsGoal)
                    .requestedBy(currentUser)
                    .dateForWithdrawal(dateForWithdrawal)
                    .withholdingTax(withHoldingTax)
                    .build();
            savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequest);
            return;
        }
        BigDecimal maximumTransactionAmount = tierLevelEntity.getBulletTransactionAmount();
        int numberOfWithdrawals =  withdrawalAmount.divide(maximumTransactionAmount, BigDecimal.ROUND_DOWN).intValue();
        BigDecimal lastWithdrawalAmountWithInterest = withdrawalAmount.remainder(maximumTransactionAmount);
        BigDecimal newCurrentBalance = currentBalance;
        for(int i = 0; i < numberOfWithdrawals; i++) {
            SavingsWithdrawalRequestEntity withdrawalRequest = SavingsWithdrawalRequestEntity.builder()
                    .amount(maximumTransactionAmount)
                    .withdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_SAVINGS_CREDIT)
                    .interestWithdrawal(BigDecimal.valueOf(0.0))
                    .savingsBalanceWithdrawal(maximumTransactionAmount)
                    .balanceBeforeWithdrawal(newCurrentBalance)
                    .maturedGoal(maturedGoal)
                    .savingsGoal(savingsGoal)
                    .requestedBy(currentUser)
                    .withholdingTax(BigDecimal.ZERO)
                    .dateForWithdrawal(dateForWithdrawal)
                    .build();
            savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequest);
            newCurrentBalance = newCurrentBalance.subtract(maximumTransactionAmount);
        }
        if(lastWithdrawalAmountWithInterest.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal withdrawalInterest = accruedInterest.subtract(remainingInterest);
            SavingsWithdrawalRequestEntity withdrawalRequest = SavingsWithdrawalRequestEntity.builder()
                    .amount(lastWithdrawalAmountWithInterest)
                    .withdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_INTEREST_CREDIT)
                    .interestWithdrawal(withdrawalInterest)
                    .balanceBeforeWithdrawal(newCurrentBalance)
                    .savingsBalanceWithdrawal(lastWithdrawalAmountWithInterest.subtract(withdrawalInterest))
                    .maturedGoal(maturedGoal)
                    .savingsGoal(savingsGoal)
                    .requestedBy(currentUser)
                    .dateForWithdrawal(dateForWithdrawal)
                    .withholdingTax(withHoldingTax)
                    .build();
            savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequest);
        }
    }

    @Override
    public void processWithHoldingTaxCharge() {
        List<SavingsWithdrawalRequestEntity> withdrawalRequestEntityList = savingsWithdrawalRequestEntityDao.getSavingsWithdrawalByStatus(WithdrawalRequestStatusConstant.PENDING_TAX_DEBIT);
        if(!withdrawalRequestEntityList.isEmpty()) {
            log.info("Withdrawal request pending tax charge: {}", withdrawalRequestEntityList.size());
        }
        for(SavingsWithdrawalRequestEntity withdrawalRequestEntity: withdrawalRequestEntityList) {

            withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PROCESSING_TAX_DEBIT);
            savingsWithdrawalRequestEntityDao.saveAndFlush(withdrawalRequestEntity);
            BigDecimal withHoldingTax = withdrawalRequestEntity.getWithholdingTax();
            if(withHoldingTax == null || withHoldingTax.compareTo(BigDecimal.ZERO) == 0) {
                withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_SAVINGS_CREDIT);
                savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                continue;
            }
            SavingsGoalEntity savingsGoalEntity = savingsGoalEntityDao.getRecordById(withdrawalRequestEntity.getSavingsGoal().getId());
            MintAccountEntity mintAccountEntity = mintAccountEntityDao.getRecordById(savingsGoalEntity.getMintAccount().getId());
            MintBankAccountEntity creditAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(mintAccountEntity, BankAccountTypeConstant.CURRENT);
            String reference = savingsWithdrawalRequestEntityDao.generateTransactionReference();
            withdrawalRequestEntity.setWhtDebitReference(reference);
            savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);

            String savingsType = getSavingsWithdrawalType(savingsGoalEntity);

            boolean usev2Endpoint = true;
            String narration = "SGIW - "+savingsGoalEntity.getGoalId()+" WHT charge";
            String withdrawalType = SavingsWithdrawalType.TAX_CHARGE_TO_CUSTOMER.name();

            SavingsWithdrawalRequestCBS withdrawalRequestCBS = SavingsWithdrawalRequestCBS.builder()
                    .accountNumber(creditAccount.getAccountNumber())
                    .amount(withHoldingTax)
                    .goalId(savingsGoalEntity.getGoalId())
                    .reference(reference)
                    .narration(narration)
                    .savingsType(savingsType)
                    .withdrawalType(withdrawalType)
                    .build();

            MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processSavingsWithdrawal(withdrawalRequestCBS, usev2Endpoint);
            if(!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value()) {
                withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.TAX_DEBIT_FAILED);
                savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                String message = String.format("Goal Id: %s; withdrawal Id: %s ; message: %s", savingsGoalEntity.getGoalId(), withdrawalRequestEntity.getId(), msClientResponse.getMessage());
                systemIssueLogService.logIssue("WHT charge Failed", "Savings WHT Charge failed", message);
                continue;
            }
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            withdrawalRequestEntity.setWhtDebitResponseCode(responseCBS.getResponseCode());
            if("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                String savingsCreditCode = withdrawalRequestEntity.getSavingsCreditResponseCode();
                if(StringUtils.isEmpty(savingsCreditCode) || !"00".equalsIgnoreCase(savingsCreditCode)) {
                    withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_SAVINGS_CREDIT);
                    savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                }else {
                    withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PROCESSED_TAX_DEBIT);
                    savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                    String message = String.format("Goal Id: %s; withdrawal Id: %s ; Unable to resolve next status of withdrawal: %s", savingsGoalEntity.getGoalId(), withdrawalRequestEntity.getId(), withdrawalRequestEntity.getWithdrawalRequestStatus());
                    systemIssueLogService.logIssue("Processed WHT debit","Processed WHT debit", message);
                }
            }else {
                withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.TAX_DEBIT_FAILED);
                savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                String message = String.format("Goal Id: %s; withdrawal Id: %s ; response code: %s;  response message: %s", savingsGoalEntity.getGoalId(), withdrawalRequestEntity.getId(), responseCBS.getResponseCode(), responseCBS.getResponseMessage());
                systemIssueLogService.logIssue("WHT Charge Failed","WHT Charge failed", message);
            }
        }
    }

    @Override
    public void processInterestWithdrawalToSuspenseAccount() {
         List<SavingsWithdrawalRequestEntity> withdrawalRequestEntityList = savingsWithdrawalRequestEntityDao.getSavingsWithdrawalByStatus(WithdrawalRequestStatusConstant.PENDING_INTEREST_CREDIT);
         if(!withdrawalRequestEntityList.isEmpty()) {
             log.info("Withdrawal request pending interest credit: {}", withdrawalRequestEntityList.size());
         }
         for(SavingsWithdrawalRequestEntity withdrawalRequestEntity: withdrawalRequestEntityList) {

             if(!withdrawalRequestEntity.isMaturedGoal()){
                 LocalDateTime dateForWithdrawal = withdrawalRequestEntity.getDateForWithdrawal().atTime(LocalTime.of(9, 0));
                 LocalDateTime now = LocalDateTime.now();
                 if(now.isBefore(dateForWithdrawal)) {
                     log.info("Withdrawal time  {} not yet reached {}", dateForWithdrawal, now);
                     return;
                 }
             }
             withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PROCESSING_INTEREST_CREDIT);
             savingsWithdrawalRequestEntityDao.saveAndFlush(withdrawalRequestEntity);
             /*if(!withdrawalRequestEntity.isMaturedGoal()) {
                   log.info("no interest will be withdrawn. Goal is not matured. {}", withdrawalRequestEntity.getId());
                   withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_SAVINGS_CREDIT);
                   savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                   continue;
             }*/
             BigDecimal withHoldingTax = withdrawalRequestEntity.getWithholdingTax();
             if(withdrawalRequestEntity.getInterestWithdrawal().compareTo(BigDecimal.ZERO) == 0) {
                 log.info("Interest Withdrawal value is zero. No interest withdrawal. {}", withdrawalRequestEntity.getId());
                 if(withHoldingTax != null && withHoldingTax.compareTo(BigDecimal.ZERO) > 0){
                     withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_TAX_DEBIT);
                 }else {
                     withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_SAVINGS_CREDIT);
                 }
                 savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                 continue;
             }
             SavingsGoalEntity savingsGoalEntity = savingsGoalEntityDao.getRecordById(withdrawalRequestEntity.getSavingsGoal().getId());
             MintAccountEntity mintAccountEntity = mintAccountEntityDao.getRecordById(savingsGoalEntity.getMintAccount().getId());
             MintBankAccountEntity creditAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(mintAccountEntity, BankAccountTypeConstant.CURRENT);
             String reference = savingsWithdrawalRequestEntityDao.generateTransactionReference();
             withdrawalRequestEntity.setInterestCreditReference(reference);
             savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);

             String savingsType = getSavingsWithdrawalType(savingsGoalEntity);

             boolean usev2Endpoint = useV2SavingsWithdrawal(withdrawalRequestEntity, savingsGoalEntity);
             String narration = "SGIW - "+savingsGoalEntity.getGoalId()+" interest payout";
             String withdrawalType = usev2Endpoint ? SavingsWithdrawalType.INTEREST_TO_CUSTOMER.name() : SavingsWithdrawalType.INTEREST_TO_SUSPENSE.name();

             SavingsWithdrawalRequestCBS withdrawalRequestCBS = SavingsWithdrawalRequestCBS.builder()
                     .accountNumber(creditAccount.getAccountNumber())
                     .amount(withdrawalRequestEntity.getInterestWithdrawal())
                     .goalId(savingsGoalEntity.getGoalId())
                     .reference(reference)
                     .narration(narration)
                     .savingsType(savingsType)
                     .withdrawalType(withdrawalType)
                     .build();

             MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processSavingsWithdrawal(withdrawalRequestCBS, usev2Endpoint);
             if(!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value()) {
                 withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.INTEREST_CREDITING_FAILED);
                 savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                 String message = String.format("Goal Id: %s; withdrawal Id: %s ; message: %s", savingsGoalEntity.getGoalId(), withdrawalRequestEntity.getId(), msClientResponse.getMessage());
                 systemIssueLogService.logIssue("Interest Withdrawal Failed", "Interest To Suspense Withdrawal failed", message);
                 if(msClientResponse.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                     publishSavingsGoalRecord(savingsGoalEntity, creditAccount, withdrawalRequestEntity);
                 }
                 continue;
             }
             FundTransferResponseCBS responseCBS = msClientResponse.getData();
             withdrawalRequestEntity.setInterestCreditResponseCode(responseCBS.getResponseCode());
             if("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                 String savingsCreditCode = withdrawalRequestEntity.getSavingsCreditResponseCode();
                 if(StringUtils.isEmpty(savingsCreditCode) || !"00".equalsIgnoreCase(savingsCreditCode)) {
                     if(withHoldingTax != null && withHoldingTax.compareTo(BigDecimal.ZERO) > 0) {
                         withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_TAX_DEBIT);
                     }else {
                         withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_SAVINGS_CREDIT);
                     }
                     savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                 }else {
                     withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PROCESSED_INTEREST_CREDIT);
                     savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                     String message = String.format("Goal Id: %s; withdrawal Id: %s ; Unable to resolve next status of withdrawal: %s", savingsGoalEntity.getGoalId(), withdrawalRequestEntity.getId(), withdrawalRequestEntity.getWithdrawalRequestStatus());
                     systemIssueLogService.logIssue("Interest Withdrawal Failed","Interest To Suspense Withdrawal failed", message);
                 }
             }else {
                 withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.INTEREST_CREDITING_FAILED);
                 savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                 String message = String.format("Goal Id: %s; withdrawal Id: %s ; response code: %s;  response message: %s", savingsGoalEntity.getGoalId(), withdrawalRequestEntity.getId(), responseCBS.getResponseCode(), responseCBS.getResponseMessage());
                 systemIssueLogService.logIssue("Interest Withdrawal Failed","Interest To Suspense Withdrawal failed", message);
             }
         }
    }

    private boolean useV2SavingsWithdrawal(SavingsWithdrawalRequestEntity withdrawalRequest, SavingsGoalEntity savingsGoal){
        boolean usev2 = withdrawalRequest.getWithholdingTax() != null && withdrawalRequest.getWithholdingTax().compareTo(BigDecimal.ZERO) > 0;
        if(usev2) {
            return true;
        }
        LocalDateTime whtChargeStartDate = LocalDateTime.of(2023, 10, 15, 0, 0, 0);
        if(savingsGoal.getDateCreated().isAfter(whtChargeStartDate) && savingsGoal.getSavingsGoalType() == SavingsGoalTypeConstant.CUSTOMER_SAVINGS) {
            return true;
        }
        return false;
    }

    private void publishSavingsGoalRecord(SavingsGoalEntity savingsGoalEntity, MintBankAccountEntity accountEntity, SavingsWithdrawalRequestEntity withdrawalRequestEntity) {
        SavingsGoalCreationEvent goalCreationEvent = SavingsGoalCreationEvent.builder()
                .goalId(savingsGoalEntity.getGoalId())
                .accruedInterest(withdrawalRequestEntity.getInterestWithdrawal())
                .savingsBalance(withdrawalRequestEntity.getBalanceBeforeWithdrawal())
                .name(savingsGoalEntity.getName())
                .withdrawalAccountNumber(accountEntity.getAccountNumber())
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.SAVING_GOAL_CREATION, new EventModel<>(goalCreationEvent));
    }

    private String getSavingsWithdrawalType(SavingsGoalEntity savingsGoalEntity) {
        String savingsType = "CUSTOMER";
        SavingsGoalTypeConstant goalType = savingsGoalEntity.getSavingsGoalType();
        if(goalType == SavingsGoalTypeConstant.MINT_REFERRAL_EARNINGS){
            savingsType = "MINT";
        }
        return savingsType;
    }

    @Override
    public void processSavingsWithdrawalToSuspenseAccount() {
        List<SavingsWithdrawalRequestEntity> withdrawalRequestEntityList = savingsWithdrawalRequestEntityDao.getSavingsWithdrawalByStatus(WithdrawalRequestStatusConstant.PENDING_SAVINGS_CREDIT);
        if(!withdrawalRequestEntityList.isEmpty()) {
            log.info("Withdrawal request pending savings withdrawal: {}", withdrawalRequestEntityList.size());
        }
        for(SavingsWithdrawalRequestEntity withdrawalRequestEntity: withdrawalRequestEntityList) {
            withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PROCESSING_SAVINGS_CREDIT);
            savingsWithdrawalRequestEntityDao.saveAndFlush(withdrawalRequestEntity);
            BigDecimal savingsAmount = withdrawalRequestEntity.getSavingsBalanceWithdrawal();

            SavingsGoalEntity goalEntity = savingsGoalEntityDao.getRecordById(withdrawalRequestEntity.getSavingsGoal().getId());
            MintAccountEntity mintAccountEntity = mintAccountEntityDao.getRecordById(goalEntity.getMintAccount().getId());
            MintBankAccountEntity creditAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(mintAccountEntity, BankAccountTypeConstant.CURRENT);
            String reference = savingsWithdrawalRequestEntityDao.generateTransactionReference();
            withdrawalRequestEntity.setSavingsCreditReference(reference);
            savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
            String savingsType = getSavingsWithdrawalType(goalEntity);

            boolean useV2Endpoint = useV2SavingsWithdrawal(withdrawalRequestEntity, goalEntity);
            String narration = "SGIW - "+goalEntity.getGoalId()+" savings payout";
            String withdrawalType = useV2Endpoint ? SavingsWithdrawalType.SAVINGS_TO_CUSTOMER.name() : SavingsWithdrawalType.SAVINGS_TO_SUSPENSE.name();

            SavingsWithdrawalRequestCBS withdrawalRequestCBS = SavingsWithdrawalRequestCBS.builder()
                    .accountNumber(creditAccount.getAccountNumber())
                    .amount(withdrawalRequestEntity.getSavingsBalanceWithdrawal())
                    .goalId(goalEntity.getGoalId())
                    .reference(reference)
                    .narration(narration)
                    .savingsType(savingsType)
                    .withdrawalType(withdrawalType)
                    .build();

            MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processSavingsWithdrawal(withdrawalRequestCBS, useV2Endpoint);
            if(!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value()) {
                withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.SAVINGS_CREDITING_FAILED);
                savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                String message = String.format("Goal Id: %s; withdrawal Id: %s ; message: %s", goalEntity.getGoalId(), withdrawalRequestEntity.getId(), msClientResponse.getMessage());
                systemIssueLogService.logIssue("Savings Withdrawal Failed", "Savings To Suspense Withdrawal failed", message);
                if(msClientResponse.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                    publishSavingsGoalRecord(goalEntity, creditAccount, withdrawalRequestEntity);
                }
                continue;
            }
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            withdrawalRequestEntity.setSavingsCreditResponseCode(responseCBS.getResponseCode());
            if("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                if(useV2Endpoint) {
                    withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PROCESSED);
                    savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                    BigDecimal amountRequest = withdrawalRequestEntity.getAmount();
                    BigDecimal totalAmountWithdrawn = (goalEntity.getTotalAmountWithdrawn() == null ? BigDecimal.ZERO : goalEntity.getTotalAmountWithdrawn()).add(amountRequest);
                    goalEntity.setTotalAmountWithdrawn(totalAmountWithdrawn);
                    if(goalEntity.getCreationSource() == SavingsGoalCreationSourceConstant.CUSTOMER) {
                        if(goalEntity.getGoalStatus() == SavingsGoalStatusConstant.COMPLETED) {
                            goalEntity.setGoalStatus(SavingsGoalStatusConstant.WITHDRAWN);
                        }
                    }
                    savingsGoalEntityDao.saveRecord(goalEntity);
                    sendNotificationForWithdrawalSavings(goalEntity, withdrawalRequestEntity, mintAccountEntity, creditAccount);
                }else {
                    withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_FUND_DISBURSEMENT);
                    savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                }
            }else {
                withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.SAVINGS_CREDITING_FAILED);
                savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                String message = String.format("Goal Id: %s; withdrawal Id: %s ; response code: %s;  response message: %s", goalEntity.getGoalId(), withdrawalRequestEntity.getId(), responseCBS.getResponseCode(), responseCBS.getResponseMessage());
                systemIssueLogService.logIssue("Savings Withdrawal Failed", "Savings To Suspense Withdrawal failed", message);
            }
        }

    }

    @Override
    public void processSuspenseFundDisbursementToCustomer() {
        List<SavingsWithdrawalRequestEntity> withdrawalRequestEntityList = savingsWithdrawalRequestEntityDao.getSavingsWithdrawalByStatus(WithdrawalRequestStatusConstant.PENDING_FUND_DISBURSEMENT);
        if(!withdrawalRequestEntityList.isEmpty()) {
            log.info("Withdrawal request pending interest credit: {}", withdrawalRequestEntityList.size());
        }
        for(SavingsWithdrawalRequestEntity withdrawalRequestEntity: withdrawalRequestEntityList) {
            withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PROCESSING_FUND_DISBURSEMENT);
            savingsWithdrawalRequestEntityDao.saveAndFlush(withdrawalRequestEntity);
            BigDecimal amountRequest = withdrawalRequestEntity.getAmount();

            SavingsGoalEntity savingsGoalEntity = savingsGoalEntityDao.getRecordById(withdrawalRequestEntity.getSavingsGoal().getId());
            MintAccountEntity mintAccountEntity = mintAccountEntityDao.getRecordById(savingsGoalEntity.getMintAccount().getId());
            MintBankAccountEntity creditAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(mintAccountEntity, BankAccountTypeConstant.CURRENT);

            BigDecimal balanceBeforeProcess = creditAccount.getAvailableBalance();

            SavingsGoalTransactionEntity transactionEntity = new SavingsGoalTransactionEntity();
            transactionEntity.setSavingsGoal(savingsGoalEntity);
            transactionEntity.setBankAccount(creditAccount);
            transactionEntity.setTransactionAmount(amountRequest);
            transactionEntity.setFundingSource(FundingSourceTypeConstant.MINT_ACCOUNT);
            transactionEntity.setCurrentBalance(withdrawalRequestEntity.getBalanceBeforeWithdrawal());
            transactionEntity.setTransactionType(TransactionTypeConstant.DEBIT);
            transactionEntity.setTransactionStatus(TransactionStatusConstant.PENDING);
            transactionEntity.setTransactionReference(savingsGoalTransactionEntityDao.generateTransactionReference());

            transactionEntity = savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
            withdrawalRequestEntity.setFundDisbursementTransaction(transactionEntity);
            savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);

            String narration = constructWithdrawalNarration(savingsGoalEntity);
            String savingsType = getSavingsWithdrawalType(savingsGoalEntity);
            SavingsWithdrawalRequestCBS withdrawalRequestCBS = SavingsWithdrawalRequestCBS.builder()
                    .accountNumber(creditAccount.getAccountNumber())
                    .amount(amountRequest)
                    .goalId(savingsGoalEntity.getGoalId())
                    .reference(transactionEntity.getTransactionReference())
                    .narration(narration)
                    .savingsType(savingsType)
                    .withdrawalType(SavingsWithdrawalType.SUSPENSE_TO_CUSTOMER.name())
                    .build();


            MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processSavingsWithdrawal(withdrawalRequestCBS, false);
            if(!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value()) {
                String message = String.format("Goal Id: %s; withdrawal Id: %s ; message: %s", savingsGoalEntity.getGoalId(), withdrawalRequestEntity.getId(), msClientResponse.getMessage());
                systemIssueLogService.logIssue("Suspense Withdrawal Failed", "Suspense To Customer funding failed", message);
                transactionEntity.setTransactionStatus(TransactionStatusConstant.FAILED);
                savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
                withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.FUND_DISBURSEMENT_FAILED);
                savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                continue;
            }
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            String code = responseCBS.getResponseCode();
            transactionEntity.setTransactionResponseCode(code);
            transactionEntity.setTransactionResponseMessage(responseCBS.getResponseMessage());
            transactionEntity.setExternalReference(responseCBS.getBankOneReference());
            if("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PROCESSED);
                transactionEntity.setTransactionStatus(TransactionStatusConstant.SUCCESSFUL);
                transactionEntity.setNewBalance(withdrawalRequestEntity.getBalanceBeforeWithdrawal().subtract(withdrawalRequestEntity.getAmount()));
                SavingsGoalEntity goalEntity = savingsGoalEntityDao.getRecordById(withdrawalRequestEntity.getSavingsGoal().getId());
                BigDecimal totalAmountWithdrawn = (goalEntity.getTotalAmountWithdrawn() == null ? BigDecimal.ZERO : goalEntity.getTotalAmountWithdrawn()).add(amountRequest);
                goalEntity.setTotalAmountWithdrawn(totalAmountWithdrawn);
                if(goalEntity.getCreationSource() == SavingsGoalCreationSourceConstant.CUSTOMER) {
                    if(goalEntity.getGoalStatus() == SavingsGoalStatusConstant.COMPLETED) {
                        goalEntity.setGoalStatus(SavingsGoalStatusConstant.WITHDRAWN);
                    }
                }
                savingsGoalEntityDao.saveRecord(goalEntity);
                sendNotificationForWithdrawalSavings(goalEntity, withdrawalRequestEntity, mintAccountEntity, creditAccount);
            }else {
                transactionEntity.setTransactionStatus(TransactionStatusConstant.FAILED);
                withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.FUND_DISBURSEMENT_FAILED);
            }
            savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
            savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
            if(transactionEntity.getTransactionStatus() == TransactionStatusConstant.SUCCESSFUL) {
                updateAccountBalanceUseCase.processBalanceUpdate(creditAccount);
            }else {
                String message = String.format("Goal Id: %s; withdrawal Id: %s ; message: %s", savingsGoalEntity.getGoalId(), withdrawalRequestEntity.getId(), msClientResponse.getMessage());
                systemIssueLogService.logIssue("Fund disbursement Failed", "savings disbursement failed", message);
            }
        }
    }

    private String constructWithdrawalNarration(SavingsGoalEntity savingsGoalEntity) {
       String narration = String.format("SGW-%s %s", savingsGoalEntity.getGoalId(), savingsGoalEntity.getName());
       if(narration.length() > 61) {
           return narration.substring(0, 60);
       }
       return narration;
    }


    private void sendNotificationForWithdrawalSavings(SavingsGoalEntity savingsGoalEntity, SavingsWithdrawalRequestEntity withdrawalRequest,
                                                      MintAccountEntity mintAccount, MintBankAccountEntity mintBankAccount) {
        LocalDateTime dateProcessed = withdrawalRequest.getDateModified();
        BigDecimal amountRequest = withdrawalRequest.getAmount();

        AppUserEntity appUserEntity = appUserEntityDao.getRecordById(savingsGoalEntity.getCreator().getId());
        if(appUserEntity.isEmailNotificationEnabled()) {
            SavingsGoalWithdrawalSuccessEvent withdrawalSuccessEvent = SavingsGoalWithdrawalSuccessEvent.builder()
                    .goalName(savingsGoalEntity.getName())
                    .amount(amountRequest)
                    .name(appUserEntity.getName())
                    .recipient(appUserEntity.getEmail())
                    .transactionDate(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                    .build();
            applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_SAVINGS_GOAL_WITHDRAWAL, new EventModel<>(withdrawalSuccessEvent));
        }
        if(appUserEntity.isGcmNotificationEnabled()) {
            if(!StringUtils.isEmpty(appUserEntity.getDeviceGcmNotificationToken())){
                String text = "Your savings withdrawal has been processed. You can access your fund from your account now.";
                PushNotificationEvent pushNotificationEvent = new PushNotificationEvent("Account Funded", text, appUserEntity.getDeviceGcmNotificationToken());
                applicationEventService.publishEvent(ApplicationEventService.EventType.PUSH_NOTIFICATION_TOKEN, new EventModel<>(pushNotificationEvent));
            }
        }
        String narration = constructWithdrawalNarration(savingsGoalEntity);
        if(appUserEntity.isSmsNotificationEnabled()) {
            updateAccountBalanceUseCase.processBalanceUpdate(mintBankAccount);
            BigDecimal balance = mintBankAccount.getAvailableBalance();
            String smsTransactionDate = dateProcessed.format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mma"));
            String creditSms = String.format("Credit\nAmt:NGN%s Cr\nAcc:%s\nDesc:%s\nTime:%s\nBal: NGN%s",
                    MoneyFormatterUtil.priceWithDecimal(amountRequest), mintBankAccount.getAccountNumber(),
                    narration, smsTransactionDate, MoneyFormatterUtil.priceWithDecimal(balance));

            SmsLogEvent creditSmsLogEvent = SmsLogEvent.builder()
                    .accountId(mintAccount.getAccountId())
                    .userId(appUserEntity.getUserId())
                    .charged(true)
                    .message(creditSms)
                    .phoneNumber(appUserEntity.getPhoneNumber())
                    .build();
            applicationEventService.publishEvent(ApplicationEventService.EventType.SMS_NOTIFICATION, new EventModel<>(creditSmsLogEvent));
        }
    }
}
