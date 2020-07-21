package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalCreationSourceConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.usecase.ComputeAvailableAmountUseCase;
import com.mintfintech.savingsms.utils.DateUtil;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Created by jnwanya on
 * Sun, 05 Jul, 2020
 */
@Named
@AllArgsConstructor
public class ComputeAvailableAmountUseCaseImpl implements ComputeAvailableAmountUseCase {

    private final ApplicationProperty applicationProperty;

    @Override
    public BigDecimal getAvailableAmount(SavingsGoalEntity savingsGoalEntity) {
        boolean matured = isMaturedSavingsGoal(savingsGoalEntity);
        if(matured) {
            return savingsGoalEntity.getSavingsBalance().add(savingsGoalEntity.getAccruedInterest());
        }
        if(savingsGoalEntity.isLockedSavings()) {
            return BigDecimal.valueOf(0.00);
        }
        long remainingDays = savingsGoalEntity.getDateCreated().until(LocalDateTime.now(), ChronoUnit.DAYS);
        int minimumDaysForWithdrawal = applicationProperty.savingsMinimumNumberOfDaysForWithdrawal();
        if(remainingDays >= minimumDaysForWithdrawal) {
            double percentageDeduction = applicationProperty.savingsInterestPercentageDeduction() / 100.0;
            BigDecimal interestDeduction = savingsGoalEntity.getAccruedInterest().multiply(BigDecimal.valueOf(percentageDeduction));
            BigDecimal availableInterest = savingsGoalEntity.getAccruedInterest().subtract(interestDeduction);
            return savingsGoalEntity.getSavingsBalance().add(availableInterest);
        }
        return BigDecimal.valueOf(0.00);
    }

    @Override
    public boolean isMaturedSavingsGoal(SavingsGoalEntity savingsGoalEntity) {
        if(savingsGoalEntity.getCreationSource() == SavingsGoalCreationSourceConstant.CUSTOMER) {
            return isMaturedCustomerGoal(savingsGoalEntity);
        }
        return isMintGoalMatured(savingsGoalEntity);
    }

    private boolean isMaturedCustomerGoal(SavingsGoalEntity savingsGoalEntity) {
        if(savingsGoalEntity.getMaturityDate() == null) {
            return false;
        }
        LocalDateTime maturityDate = savingsGoalEntity.getMaturityDate();
        if(DateUtil.sameDay(LocalDateTime.now(), maturityDate)) {
            return true;
        }
        return maturityDate.isBefore(LocalDateTime.now());
    }

    private boolean isMintGoalMatured(SavingsGoalEntity savingsGoalEntity) {
        boolean matured = false;
        if(savingsGoalEntity.getSavingsGoalType() == SavingsGoalTypeConstant.MINT_DEFAULT_SAVINGS) {
            if(applicationProperty.isProductionEnvironment()) {
                matured = BigDecimal.valueOf(1000.00).compareTo(savingsGoalEntity.getSavingsBalance()) >= 0;
            }else {
                matured = BigDecimal.valueOf(20.00).compareTo(savingsGoalEntity.getSavingsBalance()) >= 0;
            }
        }
        return matured;
    }
}
