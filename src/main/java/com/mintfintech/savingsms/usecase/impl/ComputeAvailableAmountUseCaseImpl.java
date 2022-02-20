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
import java.time.LocalDate;
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

        if(savingsGoalEntity.getSavingsGoalType() == SavingsGoalTypeConstant.EMERGENCY_SAVINGS) {
           // long days = savingsGoalEntity.getSavingsStartDate().until(LocalDate.now(), ChronoUnit.DAYS);
            /*if(applicationProperty.isProductionEnvironment() || applicationProperty.isStagingEnvironment()) {
                if(days < 30) {
                    return savingsGoalEntity.getSavingsBalance();
                }
            }else {
                if(days < 5) {
                    return savingsGoalEntity.getSavingsBalance();
                }
            }*/
            return savingsGoalEntity.getSavingsBalance().add(savingsGoalEntity.getAccruedInterest());
        }
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
            double interestDeduction = savingsGoalEntity.getAccruedInterest().doubleValue() * percentageDeduction;
           // BigDecimal interestDeduction = savingsGoalEntity.getAccruedInterest().multiply(BigDecimal.valueOf(percentageDeduction));
            double availableInterest = savingsGoalEntity.getAccruedInterest().doubleValue() - interestDeduction;
           // BigDecimal availableInterest = savingsGoalEntity.getAccruedInterest().subtract(interestDeduction);
            BigDecimal availableForWithdrawal = BigDecimal.valueOf(savingsGoalEntity.getSavingsBalance().doubleValue() + availableInterest);
           // return savingsGoalEntity.getSavingsBalance().add(availableInterest);
            return availableForWithdrawal.setScale(2, BigDecimal.ROUND_FLOOR);
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
            if(savingsGoalEntity.getSavingsGoalType() == SavingsGoalTypeConstant.EMERGENCY_SAVINGS) {
                return savingsGoalEntity.getSavingsBalance().compareTo(BigDecimal.ZERO) > 0;
            }
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
        if(savingsGoalEntity.getSavingsGoalType() == SavingsGoalTypeConstant.MINT_REFERRAL_EARNINGS) {
            if(applicationProperty.isLiveEnvironment()) {
                matured = BigDecimal.valueOf(1000.00).compareTo(savingsGoalEntity.getSavingsBalance()) <= 0;
            }else {
                matured = BigDecimal.valueOf(1000.00).compareTo(savingsGoalEntity.getSavingsBalance()) <= 0;
            }
        }else if(savingsGoalEntity.getSavingsGoalType() == SavingsGoalTypeConstant.SPEND_AND_SAVE) {
            boolean hasFund = savingsGoalEntity.getSavingsBalance().compareTo(BigDecimal.ZERO) > 0;
            if(!applicationProperty.isLiveEnvironment()) {
                 long daysPassed = savingsGoalEntity.getDateCreated().until(LocalDateTime.now(), ChronoUnit.DAYS);
                 matured = daysPassed > 4 && hasFund;
            }else {
                LocalDateTime maturityDate = savingsGoalEntity.getMaturityDate();
                if(DateUtil.sameDay(LocalDateTime.now(), maturityDate)) {
                    matured = hasFund;
                }else {
                    matured = maturityDate.isBefore(LocalDateTime.now()) && hasFund;
                }
            }
        }
        return matured;
    }
}
