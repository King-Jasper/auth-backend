package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.InterestWithdrawalRequestCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.MintFundTransferRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.FundWithdrawalUseCase;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalWithdrawalSuccessEvent;
import com.mintfintech.savingsms.usecase.data.request.SavingsWithdrawalRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.utils.DateUtil;
import com.mintfintech.savingsms.utils.MoneyFormatterUtil;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import javax.inject.Named;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
    private SavingsPlanEntityDao savingsPlanEntityDao;
    private MintBankAccountEntityDao mintBankAccountEntityDao;
    private SavingsWithdrawalRequestEntityDao savingsWithdrawalRequestEntityDao;
    private SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    private UpdateBankAccountBalanceUseCase updateAccountBalanceUseCase;
    private CoreBankingServiceClient coreBankingServiceClient;
    private SystemIssueLogService systemIssueLogService;
    private ApplicationProperty applicationProperty;
    private ApplicationEventService applicationEventService;
    private TierLevelEntityDao tierLevelEntityDao;

    @Override
    public String withdrawalSavings(AuthenticatedUser authenticatedUser, SavingsWithdrawalRequest withdrawalRequest) {
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        AppUserEntity appUserEntity = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());

        BigDecimal amountRequested = BigDecimal.valueOf(withdrawalRequest.getAmount());
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

        if(savingsGoal.getSavingsGoalType() != SavingsGoalTypeConstant.CUSTOMER_SAVINGS) {
            return processMintSavingsWithdrawal(savingsGoal, creditAccount, appUserEntity, amountRequested);
        }
        if(savingsGoal.getGoalStatus() != SavingsGoalStatusConstant.ACTIVE && savingsGoal.getGoalStatus() != SavingsGoalStatusConstant.MATURED) {
            throw new BusinessLogicConflictException("Sorry, savings withdrawal not currently supported.");
        }
        return processCustomerSavingWithdrawal(savingsGoal, appUserEntity, amountRequested);
    }

    private String processMintSavingsWithdrawal(SavingsGoalEntity savingsGoal, MintBankAccountEntity creditAccount, AppUserEntity currentUser, BigDecimal amountRequested) {
        if(savingsGoal.getSavingsGoalType() != SavingsGoalTypeConstant.MINT_DEFAULT_SAVINGS){
            throw new BusinessLogicConflictException("Sorry, fund withdrawal not yet activated");
        }
        BigDecimal accruedInterest = savingsGoal.getAccruedInterest();
        BigDecimal savingsBalance = savingsGoal.getSavingsBalance();
        BigDecimal minimumWithdrawalBalance = applicationProperty.isProductionEnvironment() ? BigDecimal.valueOf(1000.00) : BigDecimal.valueOf(20.00);
        boolean matured = minimumWithdrawalBalance.compareTo(savingsBalance) <= 0;
        if(!matured) {
            throw new BusinessLogicConflictException("Sorry, can you only withdraw when your balance is up to N"+MoneyFormatterUtil.priceWithDecimal(minimumWithdrawalBalance));
        }
        BigDecimal totalAvailableAmount = savingsBalance.add(accruedInterest);
        if(amountRequested.compareTo(totalAvailableAmount) > 0) {
            throw new BadRequestException("Amount requested ("+MoneyFormatterUtil.priceWithDecimal(amountRequested)+") cannot be above total available balance ("+MoneyFormatterUtil.priceWithDecimal(totalAvailableAmount)+")");
        }
        BigDecimal remainingInterest, remainingSavings;
        int outcome = amountRequested.compareTo(savingsBalance);
        if(outcome > 0) {
            remainingInterest = totalAvailableAmount.subtract(amountRequested);
            remainingSavings = BigDecimal.ZERO;
        }else if(outcome == 0) {
            remainingInterest = accruedInterest;
            remainingSavings = BigDecimal.ZERO;
        }else {
            remainingInterest = accruedInterest;
            remainingSavings = savingsBalance.subtract(amountRequested);
        }
        savingsGoal.setSavingsBalance(remainingSavings);
        savingsGoal.setAccruedInterest(remainingInterest);
        savingsGoalEntityDao.saveRecord(savingsGoal);

        SavingsWithdrawalRequestEntity withdrawalRequest = SavingsWithdrawalRequestEntity.builder()
                .amount(amountRequested)
                .savingsBalanceWithdrawal(savingsBalance.subtract(remainingSavings))
                .withdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_INTEREST_CREDIT)
                .interestWithdrawal(accruedInterest.subtract(remainingInterest))
                .balanceBeforeWithdrawal(savingsBalance)
                .maturedGoal(true)
                .savingsGoal(savingsGoal)
                .requestedBy(currentUser)
                .build();
        savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequest);
        return "Request queued successfully. Your account will be funded very soon.";
    }

    @Transactional
    public String processCustomerSavingWithdrawal(SavingsGoalEntity savingsGoal, AppUserEntity currentUser, BigDecimal amountRequested) {
        LocalDateTime now = LocalDateTime.now();
        long remainingDays = savingsGoal.getDateCreated().until(now, ChronoUnit.DAYS);
        int minimumDaysForWithdrawal = applicationProperty.savingsMinimumNumberOfDaysForWithdrawal();
        if(remainingDays < minimumDaysForWithdrawal) {
            throw new BusinessLogicConflictException("Sorry, you have "+(minimumDaysForWithdrawal - remainingDays)+" days left before you can withdraw fund from your savings.");
        }
        boolean isMatured = DateUtil.sameDay(now, savingsGoal.getMaturityDate()) || savingsGoal.getMaturityDate().isBefore(now);
        final BigDecimal totalAvailableBalance;
        if(isMatured) {
            log.info("MATURED GOAL: {}", savingsGoal.getGoalId());
            totalAvailableBalance = savingsGoal.getSavingsBalance().add(savingsGoal.getAccruedInterest());
        }else {
            SavingsPlanEntity planEntity = savingsPlanEntityDao.getRecordById(savingsGoal.getSavingsPlan().getId());
            totalAvailableBalance = savingsGoal.getSavingsBalance().subtract(planEntity.getMinimumBalance());
        }
        System.out.println("Available amount: "+totalAvailableBalance+" amount requested: "+amountRequested);
        if(amountRequested.compareTo(totalAvailableBalance) > 0) {
            throw new BusinessLogicConflictException("Sorry, maximum amount that can be withdrawn is N"+ MoneyFormatterUtil.priceWithDecimal(totalAvailableBalance));
        }
        if(isMatured){
            amountRequested = totalAvailableBalance;
        }
        createWithdrawalRequest(savingsGoal, amountRequested, isMatured, currentUser);
        if(isMatured) {
            return "Request queued successfully. Your account will be funded very soon.";
        }
        return "Request queued successfully. Your account will be funded within the next 2 business days.";
    }

    private synchronized void createWithdrawalRequest(SavingsGoalEntity savingsGoal, BigDecimal amountRequested, boolean maturedGoal, AppUserEntity currentUser) {
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusSeconds(120);
        if(savingsWithdrawalRequestEntityDao.countWithdrawalRequestWithinPeriod(savingsGoal, twoMinutesAgo, LocalDateTime.now()) > 0) {
            throw new BusinessLogicConflictException("Possible duplicate withdrawal request.");
        }
        BigDecimal currentBalance = savingsGoal.getSavingsBalance();
        BigDecimal accruedInterest = savingsGoal.getAccruedInterest();
        BigDecimal remainingSavings, remainingInterest;
        if(maturedGoal) {
            remainingSavings = BigDecimal.valueOf(0.00);
            remainingInterest = BigDecimal.valueOf(0.00);
            savingsGoal.setGoalStatus(SavingsGoalStatusConstant.COMPLETED);
        }else {
            remainingInterest = accruedInterest;
            remainingSavings = currentBalance.subtract(amountRequested);
        }
        savingsGoal.setAccruedInterest(remainingInterest);
        savingsGoal.setSavingsBalance(remainingSavings);
        savingsGoalEntityDao.saveRecord(savingsGoal);

        MintBankAccountEntity savingAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(savingsGoal.getMintAccount(), BankAccountTypeConstant.SAVING);
        TierLevelEntity tierLevelEntity = tierLevelEntityDao.getRecordById(savingAccount.getAccountTierLevel().getId());
        if(tierLevelEntity.getLevel() == TierLevelTypeConstant.TIER_THREE || (amountRequested.compareTo(tierLevelEntity.getBulletTransactionAmount()) <= 0)) {
            // no need of splitting the withdrawal due to bullet transaction limit.
            SavingsWithdrawalRequestEntity withdrawalRequest = SavingsWithdrawalRequestEntity.builder()
                    .amount(amountRequested)
                    .withdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_INTEREST_CREDIT)
                    .interestWithdrawal(accruedInterest.subtract(remainingInterest))
                    .savingsBalanceWithdrawal(currentBalance.subtract(remainingSavings))
                    .balanceBeforeWithdrawal(currentBalance)
                    .maturedGoal(maturedGoal)
                    .savingsGoal(savingsGoal)
                    .requestedBy(currentUser)
                    .build();
            savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequest);
            return;
        }
        BigDecimal maximumTransactionAmount = tierLevelEntity.getBulletTransactionAmount();
        int numberOfWithdrawals =  amountRequested.divide(maximumTransactionAmount, BigDecimal.ROUND_DOWN).intValue();
        BigDecimal lastWithdrawalAmountWithInterest = amountRequested.remainder(maximumTransactionAmount);
        BigDecimal newCurrentBalance = currentBalance;
        for(int i = 0; i < numberOfWithdrawals; i++) {
            SavingsWithdrawalRequestEntity withdrawalRequest = SavingsWithdrawalRequestEntity.builder()
                    .amount(maximumTransactionAmount)
                    .withdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_FUND_DISBURSEMENT)
                    .interestWithdrawal(BigDecimal.valueOf(0.0))
                    .savingsBalanceWithdrawal(maximumTransactionAmount)
                    .balanceBeforeWithdrawal(newCurrentBalance)
                    .maturedGoal(maturedGoal)
                    .savingsGoal(savingsGoal)
                    .requestedBy(currentUser)
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
                    .build();
            savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequest);
        }
    }

    @Override
    public void processInterestCreditForFundWithdrawal() {
         List<SavingsWithdrawalRequestEntity> withdrawalRequestEntityList = savingsWithdrawalRequestEntityDao.getSavingsWithdrawalByStatus(WithdrawalRequestStatusConstant.PENDING_INTEREST_CREDIT);
         if(!withdrawalRequestEntityList.isEmpty()) {
             log.info("Withdrawal request pending interest credit: {}", withdrawalRequestEntityList.size());
         }
         for(SavingsWithdrawalRequestEntity withdrawalRequestEntity: withdrawalRequestEntityList) {
             withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PROCESSING_INTEREST_CREDIT);
             savingsWithdrawalRequestEntityDao.saveAndFlush(withdrawalRequestEntity);
             if(!withdrawalRequestEntity.isMaturedGoal()) {
                   log.info("no interest will be withdrawn. Goal is not matured. {}", withdrawalRequestEntity.getId());
                   withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_FUND_DISBURSEMENT);
                   savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                   continue;
             }
             if(withdrawalRequestEntity.getInterestWithdrawal().compareTo(BigDecimal.ZERO) == 0) {
                 log.info("Interest Withdrawal value is zero. No interest withdrawal. {}", withdrawalRequestEntity.getId());
                 withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_FUND_DISBURSEMENT);
                 savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                 continue;
             }
             SavingsGoalEntity savingsGoalEntity = savingsGoalEntityDao.getRecordById(withdrawalRequestEntity.getSavingsGoal().getId());
             MintAccountEntity mintAccountEntity = mintAccountEntityDao.getRecordById(savingsGoalEntity.getMintAccount().getId());
             MintBankAccountEntity savingAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(mintAccountEntity, BankAccountTypeConstant.SAVING);
             String reference = savingsWithdrawalRequestEntityDao.generateInterestTransactionReference();
             withdrawalRequestEntity.setInterestCreditReference(reference);
             savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);

             InterestWithdrawalRequestCBS requestCBS = InterestWithdrawalRequestCBS.builder()
                     .accountId(mintAccountEntity.getAccountId())
                     .goalId(savingsGoalEntity.getGoalId())
                     .accountNumber(savingAccount.getAccountNumber())
                     .interestAmount(withdrawalRequestEntity.getInterestWithdrawal().doubleValue())
                     .reference(reference)
                     .build();
             MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processSavingInterestWithdrawal(requestCBS);
             if(!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value()) {
                 withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.INTEREST_CREDITING_FAILED);
                 savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                 String message = String.format("Goal Id: %s; withdrawal Id: %s ; message: %s", savingsGoalEntity.getGoalId(), withdrawalRequestEntity.getId(), msClientResponse.getMessage());
                 systemIssueLogService.logIssue("Interest funding failed", message);
                 continue;
             }
             FundTransferResponseCBS responseCBS = msClientResponse.getData();
             withdrawalRequestEntity.setInterestCreditResponseCode(responseCBS.getResponseCode());
             if("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                withdrawalRequestEntity.setInterestCreditedOnDebitAccount(true);
                withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_FUND_DISBURSEMENT);
                savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
             }else {
                 withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.INTEREST_CREDITING_FAILED);
                 savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
                 String message = String.format("Goal Id: %s; withdrawal Id: %s ; response code: %s;  response message: %s", savingsGoalEntity.getGoalId(), withdrawalRequestEntity.getId(), responseCBS.getResponseCode(), responseCBS.getResponseMessage());
                 systemIssueLogService.logIssue("Interest funding failed", message);
             }
         }
    }

    @Override
    public void processSavingFundCrediting() {
        List<SavingsWithdrawalRequestEntity> withdrawalRequestEntityList = savingsWithdrawalRequestEntityDao.getSavingsWithdrawalByStatus(WithdrawalRequestStatusConstant.PENDING_FUND_DISBURSEMENT);
        if(!withdrawalRequestEntityList.isEmpty()) {
            log.info("Withdrawal request pending interest credit: {}", withdrawalRequestEntityList.size());
        }
        for(SavingsWithdrawalRequestEntity withdrawalRequestEntity: withdrawalRequestEntityList) {
            withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.PROCESSING_FUND_DISBURSEMENT);
            savingsWithdrawalRequestEntityDao.saveAndFlush(withdrawalRequestEntity);
            BigDecimal amountRequest = withdrawalRequestEntity.getAmount();
            /*if(withdrawalRequestEntity.isMaturedGoal() && withdrawalRequestEntity.isInterestCreditedOnDebitAccount()){
                amountRequest = amountRequest.add(withdrawalRequestEntity.getAccruedInterest());
            }*/
            SavingsGoalEntity savingsGoalEntity = savingsGoalEntityDao.getRecordById(withdrawalRequestEntity.getSavingsGoal().getId());
            MintAccountEntity mintAccountEntity = mintAccountEntityDao.getRecordById(savingsGoalEntity.getMintAccount().getId());
            MintBankAccountEntity debitAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(mintAccountEntity, BankAccountTypeConstant.SAVING);
            MintBankAccountEntity creditAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(mintAccountEntity, BankAccountTypeConstant.CURRENT);

            SavingsGoalTransactionEntity transactionEntity = SavingsGoalTransactionEntity.builder()
                    .transactionAmount(amountRequest)
                    .transactionReference(savingsGoalTransactionEntityDao.generateTransactionReference())
                    .debitAccount(debitAccount)
                    .creditAccount(creditAccount)
                    .transactionType(TransactionTypeConstant.DEBIT)
                    .transactionStatus(TransactionStatusConstant.PENDING)
                    .savingsGoal(savingsGoalEntity)
                    .currentBalance(withdrawalRequestEntity.getBalanceBeforeWithdrawal())
                    .build();

            transactionEntity = savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
            withdrawalRequestEntity.setFundDisbursementTransaction(transactionEntity);
            savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);

            MintFundTransferRequestCBS transferRequestCBS = MintFundTransferRequestCBS.builder()
                    .amount(amountRequest)
                    .creditAccountNumber(creditAccount.getAccountNumber())
                    .debitAccountNumber(debitAccount.getAccountNumber())
                    .narration("Savings withdrawal - "+savingsGoalEntity.getGoalId())
                    .transactionReference(transactionEntity.getTransactionReference())
                    .build();
            MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processMintFundTransfer(transferRequestCBS);
            if(!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value()) {
                String message = String.format("Goal Id: %s; withdrawal Id: %s ; message: %s", savingsGoalEntity.getGoalId(), withdrawalRequestEntity.getId(), msClientResponse.getMessage());
                systemIssueLogService.logIssue("Interest funding failed", message);
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

                AppUserEntity appUserEntity = appUserEntityDao.getRecordById(savingsGoalEntity.getCreator().getId());

                SavingsGoalWithdrawalSuccessEvent withdrawalSuccessEvent = SavingsGoalWithdrawalSuccessEvent.builder()
                        .goalName(savingsGoalEntity.getName())
                        .amount(amountRequest)
                        .name(appUserEntity.getName())
                        .recipient(appUserEntity.getEmail())
                        .build();
                applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_SAVINGS_GOAL_WITHDRAWAL, new EventModel<>(withdrawalSuccessEvent));
            }else {
                transactionEntity.setTransactionStatus(TransactionStatusConstant.FAILED);
                withdrawalRequestEntity.setWithdrawalRequestStatus(WithdrawalRequestStatusConstant.FUND_DISBURSEMENT_FAILED);
            }
            savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
            savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequestEntity);
            if(transactionEntity.getTransactionStatus() == TransactionStatusConstant.SUCCESSFUL) {
                updateAccountBalanceUseCase.processBalanceUpdate(mintAccountEntity);
            }
        }
    }

}
