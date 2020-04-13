package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.InterestWithdrawalRequestCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.MintFundTransferRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.FundWithdrawalUseCase;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.request.SavingsWithdrawalRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.utils.MoneyFormatterUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public String withdrawalSavings(AuthenticatedUser authenticatedUser, SavingsWithdrawalRequest withdrawalRequest) {
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        AppUserEntity appUserEntity = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());

        BigDecimal amountRequested = BigDecimal.valueOf(withdrawalRequest.getAmount());
        SavingsGoalEntity savingsGoal = savingsGoalEntityDao.findSavingGoalByAccountAndGoalId(accountEntity, withdrawalRequest.getGoalId())
                .orElseThrow(() -> new BadRequestException("Invalid savings goal Id."));

       // MintBankAccountEntity creditAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(accountEntity, BankAccountTypeConstant.CURRENT);
        if(savingsGoal.getSavingsGoalType() != SavingsGoalTypeConstant.CUSTOMER_SAVINGS) {
            throw new BusinessLogicConflictException("Sorry, fund withdrawal not yet activated");
        }
        if(savingsGoal.getGoalStatus() != SavingsGoalStatusConstant.ACTIVE && savingsGoal.getGoalStatus() != SavingsGoalStatusConstant.MATURED) {
            throw new BusinessLogicConflictException("Sorry, savings withdrawal not currently supported.");
        }
        return processCustomerSavingWithdrawal(savingsGoal, appUserEntity, amountRequested);
    }

    @Transactional
    public String processCustomerSavingWithdrawal(SavingsGoalEntity savingsGoal, AppUserEntity currentUser, BigDecimal amountRequested) {
        LocalDateTime now = LocalDateTime.now();
        long remainingDays = savingsGoal.getDateCreated().until(now, ChronoUnit.DAYS);
        int minimumDaysForWithdrawal = applicationProperty.savingsMinimumNumberOfDaysForWithdrawal();
        if(remainingDays < minimumDaysForWithdrawal) {
            throw new BusinessLogicConflictException("Sorry, you have "+(minimumDaysForWithdrawal - remainingDays)+" days left before you can withdraw fund from your savings.");
        }
        boolean isMatured = now.until(savingsGoal.getMaturityDate(), ChronoUnit.DAYS) <= 0;
        final BigDecimal withdrawableBalance;
        if(isMatured) {
            log.info("MATURED GOAL: {}", savingsGoal.getGoalId());
            amountRequested = savingsGoal.getSavingsBalance();
            withdrawableBalance = savingsGoal.getSavingsBalance();
        }else {
            SavingsPlanEntity planEntity = savingsPlanEntityDao.getRecordById(savingsGoal.getSavingsPlan().getId());
            withdrawableBalance = savingsGoal.getSavingsBalance().subtract(planEntity.getMinimumBalance());
        }
        System.out.println("withdrawal amount: "+withdrawableBalance+" amount requested: "+amountRequested);
        if(amountRequested.compareTo(withdrawableBalance) > 0) {
            throw new BusinessLogicConflictException("Sorry, maximum withdrawable balance is N"+ MoneyFormatterUtil.priceWithDecimal(withdrawableBalance));
        }
     //   createWithdrawalRequest(savingsGoal, amountRequested, isMatured, currentUser);
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
        savingsGoal.setSavingsBalance(savingsGoal.getSavingsBalance().subtract(amountRequested));
        if(maturedGoal) {
            savingsGoal.setGoalStatus(SavingsGoalStatusConstant.COMPLETED);
        }
        savingsGoalEntityDao.saveRecord(savingsGoal);
        SavingsWithdrawalRequestEntity withdrawalRequest = SavingsWithdrawalRequestEntity.builder()
                .amount(amountRequested)
                .withdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_INTEREST_CREDIT)
                .accruedInterest(savingsGoal.getAccruedInterest())
                .amountSaved(savingsGoal.getSavingsAmount())
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
                   log.info("no interest will be applied. Goal is not matured. {}", withdrawalRequestEntity.getId());
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
                    .currentBalance(withdrawalRequestEntity.getAmountSaved())
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
                transactionEntity.setNewBalance(savingsGoalEntity.getSavingsBalance());
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
