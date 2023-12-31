package com.mintfintech.savingsms.usecase.features.spend_and_save.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.WithdrawalRequestStatusConstant;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.ComputeAvailableAmountUseCase;
import com.mintfintech.savingsms.usecase.data.request.SpendAndSaveWithdrawalRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.spend_and_save.WithdrawSpendAndSaveUseCase;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDate;

@Named
@AllArgsConstructor
public class WithdrawSpendAndSaveUseCaseImpl implements WithdrawSpendAndSaveUseCase {

    private final AppUserEntityDao appUserEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final SpendAndSaveEntityDao spendAndSaveEntityDao;
    private final ComputeAvailableAmountUseCase computeAvailableAmountUseCase;
    private final SavingsGoalEntityDao savingsGoalEntityDao;
    private final SavingsWithdrawalRequestEntityDao savingsWithdrawalRequestEntityDao;


    @Override
    public String withdrawSavings(AuthenticatedUser authenticatedUser, SpendAndSaveWithdrawalRequest withdrawalRequest) {
        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());

        SpendAndSaveEntity spendAndSave = spendAndSaveEntityDao.findSpendAndSaveByAppUserAndMintAccount(appUser, mintAccount)
                .orElseThrow(() -> new BadRequestException("Invalid account"));

        SavingsGoalEntity savingsGoal = spendAndSave.getSavings();
        if (spendAndSave.isSavingsLocked()) {
            if (!computeAvailableAmountUseCase.isMaturedSavingsGoal(savingsGoal)) {
                throw new BusinessLogicConflictException("Your savings is not matured yet.");
            }
        }
        BigDecimal amount = BigDecimal.valueOf(withdrawalRequest.getAmount());
        String creditAccountId = withdrawalRequest.getCreditAccountId();
        if (StringUtils.isEmpty(creditAccountId)) {
            throw new BadRequestException("Credit Account Id is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("Amount should be greater than 0.0");
        }

        if (savingsGoal.getSavingsGoalType() != SavingsGoalTypeConstant.SPEND_AND_SAVE) {
            throw new BusinessLogicConflictException("Sorry, your savings type is not Spend and Save");
        }
        return createWithdrawalRequest(spendAndSave, savingsGoal, appUser, amount);
    }

    private String createWithdrawalRequest(SpendAndSaveEntity spendAndSave, SavingsGoalEntity savingsGoal, AppUserEntity appUser, BigDecimal amount) {


        // 1000, 50 - 1050

        LocalDate dateForWithdrawal = LocalDate.now();
        BigDecimal currentBalance = savingsGoal.getSavingsBalance();
        BigDecimal accruedInterest = savingsGoal.getAccruedInterest();
        BigDecimal remainingSavings = BigDecimal.valueOf(0.00), remainingInterest = BigDecimal.valueOf(0.00);

        BigDecimal totalAmount = currentBalance.add(accruedInterest);
        if (amount.compareTo(totalAmount) > 0) {
            throw new BusinessLogicConflictException("Sorry, amount is greater than the available amount");
        }
        /*
        if (amount.compareTo(accruedInterest) > 0 && amount.compareTo(totalAmount) < 0) {
            remainingSavings = totalAmount.subtract(amount);
        } else if (amount.compareTo(accruedInterest) < 0) {
            remainingInterest = accruedInterest.subtract(amount);
            remainingSavings = currentBalance;
        }*/
        if(amount.compareTo(totalAmount) != 0) {
            if(amount.compareTo(currentBalance) < 0) {
                remainingInterest = accruedInterest;
                remainingSavings = currentBalance.subtract(amount);
            }else {
                remainingSavings = BigDecimal.valueOf(0.00);
                remainingInterest = totalAmount.subtract(amount);
            }
        }else {
            if(!spendAndSave.isActivated()) {
                savingsGoal.setRecordStatus(RecordStatusConstant.INACTIVE);
                spendAndSave.setRecordStatus(RecordStatusConstant.INACTIVE);
                spendAndSaveEntityDao.saveRecord(spendAndSave);
            }
        }

        savingsGoal.setSavingsBalance(remainingSavings);
        savingsGoal.setAccruedInterest(remainingInterest);
        savingsGoalEntityDao.saveRecord(savingsGoal);

        BigDecimal interestWithdrawal = accruedInterest.subtract(remainingInterest);
        WithdrawalRequestStatusConstant requestStatusConstant = WithdrawalRequestStatusConstant.PENDING_SAVINGS_CREDIT;
        if(interestWithdrawal.compareTo(BigDecimal.ZERO) > 0) {
            requestStatusConstant = WithdrawalRequestStatusConstant.PENDING_INTEREST_CREDIT;
        }

        //

        //withdrawalRequestEntity.getSavingsBalanceWithdrawal()

        SavingsWithdrawalRequestEntity withdrawalRequest = SavingsWithdrawalRequestEntity.builder()
                .amount(amount)
                .withdrawalRequestStatus(requestStatusConstant)
                .interestWithdrawal(interestWithdrawal)
                .savingsBalanceWithdrawal(currentBalance.subtract(remainingSavings))
                .balanceBeforeWithdrawal(currentBalance)
                .maturedGoal(true)
                .savingsGoal(savingsGoal)
                .requestedBy(appUser)
                .dateForWithdrawal(dateForWithdrawal)
                .build();
        savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequest);
        return "Request queued successfully. Your account will be funded shortly.";
    }
}
