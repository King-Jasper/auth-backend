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
        throw new BusinessLogicConflictException("Sorry, fund withdrawal not yet activated");
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
        final BigDecimal availableBalance;
        if(isMatured) {
            log.info("MATURED GOAL: {}", savingsGoal.getGoalId());
            availableBalance = savingsGoal.getSavingsBalance().add(savingsGoal.getAccruedInterest());
        }else {
            SavingsPlanEntity planEntity = savingsPlanEntityDao.getRecordById(savingsGoal.getSavingsPlan().getId());
            availableBalance = savingsGoal.getSavingsBalance().subtract(planEntity.getMinimumBalance());
        }
        System.out.println("Available amount: "+availableBalance+" amount requested: "+amountRequested);
        if(amountRequested.compareTo(availableBalance) > 0) {
            throw new BusinessLogicConflictException("Sorry, maximum amount that can be withdrawn is N"+ MoneyFormatterUtil.priceWithDecimal(availableBalance));
        }
        if(isMatured){
            amountRequested = savingsGoal.getSavingsBalance();
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
        savingsGoal.setSavingsBalance(currentBalance.subtract(amountRequested));
        BigDecimal accruedInterest = savingsGoal.getAccruedInterest();
        if(maturedGoal) {
            savingsGoal.setGoalStatus(SavingsGoalStatusConstant.COMPLETED);
            savingsGoal.setAccruedInterest(BigDecimal.valueOf(0.00));
        }
        savingsGoalEntityDao.saveRecord(savingsGoal);
        SavingsWithdrawalRequestEntity withdrawalRequest = SavingsWithdrawalRequestEntity.builder()
                .amount(amountRequested)
                .withdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_INTEREST_CREDIT)
                .accruedInterest(accruedInterest)
                .balanceBeforeWithdrawal(currentBalance)
                .maturedGoal(maturedGoal)
                .savingsGoal(savingsGoal)
                .requestedBy(currentUser)
                .build();
        savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequest);
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
                     .interestAmount(withdrawalRequestEntity.getAccruedInterest().doubleValue())
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
            if(withdrawalRequestEntity.isMaturedGoal() && withdrawalRequestEntity.isInterestCreditedOnDebitAccount()){
                amountRequest = amountRequest.add(withdrawalRequestEntity.getAccruedInterest());
            }
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
