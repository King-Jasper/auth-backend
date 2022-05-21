package com.mintfintech.savingsms.usecase.features.emergency_savings.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TierLevelTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.WithdrawalRequestStatusConstant;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.SavingsWithdrawalRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.emergency_savings.WithdrawEmergencySavingsUseCase;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@FieldDefaults(makeFinal = true)
@Slf4j
@Named
@AllArgsConstructor
public class WithdrawEmergencySavingsUseCaseImpl implements WithdrawEmergencySavingsUseCase {

    private AppUserEntityDao appUserEntityDao;
    private MintAccountEntityDao mintAccountEntityDao;
    private SavingsGoalEntityDao savingsGoalEntityDao;
    private MintBankAccountEntityDao mintBankAccountEntityDao;
    private SavingsWithdrawalRequestEntityDao savingsWithdrawalRequestEntityDao;
    private TierLevelEntityDao tierLevelEntityDao;


    @Override
    public String withdrawalSavingsV2(AuthenticatedUser authenticatedUser, SavingsWithdrawalRequest withdrawalRequest) {

        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        AppUserEntity appUserEntity = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());

        BigDecimal amountRequested = BigDecimal.valueOf(withdrawalRequest.getAmount());
        SavingsGoalEntity savingsGoal = savingsGoalEntityDao.findSavingGoalByAccountAndGoalId(accountEntity, withdrawalRequest.getGoalId())
                .orElseThrow(() -> new BadRequestException("Invalid savings goal Id."));

        if(savingsGoal.getSavingsBalance().compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("Sorry, you have zero savings balance");
        }
        if(amountRequested.compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("Withdrawal amount cannot be zero balance");
        }
        if (savingsGoal.getSavingsBalance().compareTo(amountRequested) < 0) {
            throw new BadRequestException("Sorry, your current savings balance is less than the amount requested.");
        }
        return processEmergencySavingsWithdrawalV2(savingsGoal, appUserEntity, amountRequested);
    }

    @Transactional
    public String processEmergencySavingsWithdrawalV2(SavingsGoalEntity savingsGoal, AppUserEntity currentUser, BigDecimal amountRequested) {
        createWithdrawalRequestV2(savingsGoal, currentUser, amountRequested);
        return "Request queued successfully. Your account will be funded shortly.";
    }

    private synchronized void createWithdrawalRequestV2(SavingsGoalEntity savingsGoal, AppUserEntity currentUser, BigDecimal amountRequested) {
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusSeconds(120);
        if(savingsWithdrawalRequestEntityDao.countWithdrawalRequestWithinPeriod(savingsGoal, twoMinutesAgo, LocalDateTime.now()) > 0) {
            throw new BusinessLogicConflictException("Possible duplicate withdrawal request.");
        }

        LocalDate dateForWithdrawal = LocalDate.now();
        BigDecimal currentBalance = savingsGoal.getSavingsBalance();
        BigDecimal newBalance = currentBalance.subtract(amountRequested);
        BigDecimal remainingSavings = currentBalance.subtract(amountRequested);
        savingsGoal.setSavingsBalance(newBalance);
        System.out.println("NEW BALANCE - "+newBalance);
        if(newBalance.compareTo(BigDecimal.ZERO) > 0) {
            savingsGoal.setGoalStatus(SavingsGoalStatusConstant.ACTIVE);
        }else {
            savingsGoal.setGoalStatus(SavingsGoalStatusConstant.COMPLETED);
        }
        savingsGoalEntityDao.saveRecord(savingsGoal);

        MintBankAccountEntity creditAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(savingsGoal.getMintAccount(), BankAccountTypeConstant.CURRENT);
        TierLevelEntity tierLevelEntity = tierLevelEntityDao.getRecordById(creditAccount.getAccountTierLevel().getId());
        if(tierLevelEntity.getLevel() == TierLevelTypeConstant.TIER_THREE || (amountRequested.compareTo(tierLevelEntity.getBulletTransactionAmount()) <= 0)) {
            // no need of splitting the withdrawal due to bullet transaction limit.
            SavingsWithdrawalRequestEntity withdrawalRequest = SavingsWithdrawalRequestEntity.builder()
                    .amount(amountRequested)
                    .withdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_INTEREST_CREDIT)
                    .interestWithdrawal(BigDecimal.ZERO)
                    .savingsBalanceWithdrawal(currentBalance.subtract(remainingSavings))
                    .balanceBeforeWithdrawal(currentBalance)
                    .maturedGoal(true)
                    .savingsGoal(savingsGoal)
                    .requestedBy(currentUser)
                    .dateForWithdrawal(dateForWithdrawal)
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
                    .withdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_SAVINGS_CREDIT)
                    .interestWithdrawal(BigDecimal.valueOf(0.0))
                    .savingsBalanceWithdrawal(maximumTransactionAmount)
                    .balanceBeforeWithdrawal(newCurrentBalance)
                    .maturedGoal(true)
                    .savingsGoal(savingsGoal)
                    .requestedBy(currentUser)
                    .dateForWithdrawal(dateForWithdrawal)
                    .build();
            savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequest);
            newCurrentBalance = newCurrentBalance.subtract(maximumTransactionAmount);
        }
        if(lastWithdrawalAmountWithInterest.compareTo(BigDecimal.ZERO) > 0) {
            SavingsWithdrawalRequestEntity withdrawalRequest = SavingsWithdrawalRequestEntity.builder()
                    .amount(lastWithdrawalAmountWithInterest)
                    .withdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_INTEREST_CREDIT)
                    .interestWithdrawal(BigDecimal.ZERO)
                    .balanceBeforeWithdrawal(newCurrentBalance)
                    .savingsBalanceWithdrawal(lastWithdrawalAmountWithInterest)
                    .maturedGoal(true)
                    .savingsGoal(savingsGoal)
                    .requestedBy(currentUser)
                    .dateForWithdrawal(dateForWithdrawal)
                    .build();
            savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequest);
        }
    }
}
