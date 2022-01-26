package com.mintfintech.savingsms.usecase.features.spend_and_save.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.WithdrawalRequestStatusConstant;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.ComputeAvailableAmountUseCase;
import com.mintfintech.savingsms.usecase.data.request.SpendAndSaveWithdrawalRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.spend_and_save.WithdrawSpendAndSaveUseCase;
import com.mintfintech.savingsms.utils.DateUtil;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Named
@AllArgsConstructor
public class WithdrawSpendAndSaveUseCaseImpl implements WithdrawSpendAndSaveUseCase {

    private final AppUserEntityDao appUserEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final SpendAndSaveEntityDao spendAndSaveEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final ComputeAvailableAmountUseCase computeAvailableAmountUseCase;
    private final ApplicationProperty applicationProperty;
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
            if (savingsGoal.getGoalStatus().equals(SavingsGoalStatusConstant.MATURED)) {
                throw new BusinessLogicConflictException("Your withdrawal is being processed");
            }
            throw new BusinessLogicConflictException("Sorry, your savings is locked");
        }
        BigDecimal amount = BigDecimal.valueOf(withdrawalRequest.getAmount());
        String creditAccountId = withdrawalRequest.getCreditAccountId();
        if (StringUtils.isEmpty(creditAccountId)) {
            throw new BadRequestException("Credit Account Id is required");
        }
        if (amount.equals(BigDecimal.ZERO)) {
            throw new BadRequestException("Amount should be greater than 0.0");
        }
        MintBankAccountEntity creditAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(creditAccountId, mintAccount)
                .orElseThrow(() -> new BadRequestException("Invalid credit account"));

        if (savingsGoal.getSavingsGoalType() != SavingsGoalTypeConstant.SPEND_AND_SAVE) {
            throw new BusinessLogicConflictException("Sorry, your savings type is not Spend and Save");
        }
        return createWithdrawalRequest(savingsGoal, appUser, amount);
    }

    private String createWithdrawalRequest(SavingsGoalEntity savingsGoal, AppUserEntity appUser, BigDecimal amount) {


        LocalDate dateForWithdrawal = LocalDate.now();
        BigDecimal currentBalance = savingsGoal.getSavingsBalance();
        BigDecimal accruedInterest = savingsGoal.getAccruedInterest();
        BigDecimal remainingSavings = BigDecimal.valueOf(0.00), remainingInterest = BigDecimal.valueOf(0.00);

        if (amount.compareTo(currentBalance) > 0) {
            throw new BusinessLogicConflictException("Sorry, amount is greater than the available amount");
        }
        remainingSavings = currentBalance.subtract(amount);

        savingsGoal.setSavingsBalance(remainingSavings);
        savingsGoalEntityDao.saveRecord(savingsGoal);

        SavingsWithdrawalRequestEntity withdrawalRequest = SavingsWithdrawalRequestEntity.builder()
                .amount(amount)
                .withdrawalRequestStatus(WithdrawalRequestStatusConstant.PENDING_SAVINGS_CREDIT)
                .interestWithdrawal(accruedInterest.subtract(remainingInterest))
                .savingsBalanceWithdrawal(amount)
                .balanceBeforeWithdrawal(currentBalance)
                .maturedGoal(true)
                .savingsGoal(savingsGoal)
                .requestedBy(appUser)
                .dateForWithdrawal(dateForWithdrawal)
                .build();
        savingsWithdrawalRequestEntityDao.saveRecord(withdrawalRequest);
        return "Request queued successfully. Your account will be funded within the next 2 business days.";
    }
}
