package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsWithdrawalRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.WithdrawalRequestStatusConstant;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by jnwanya on
 * Tue, 07 Apr, 2020
 */
public interface SavingsWithdrawalRequestEntityDao extends CrudDao<SavingsWithdrawalRequestEntity, Long> {
    SavingsWithdrawalRequestEntity saveAndFlush(SavingsWithdrawalRequestEntity savingsWithdrawalRequestEntity);
    String generateTransactionReference();
    long countWithdrawalRequestWithinPeriod(SavingsGoalEntity savingsGoal, LocalDateTime fromTime, LocalDateTime toTime);
    List<SavingsWithdrawalRequestEntity> getSavingsWithdrawalByStatus(WithdrawalRequestStatusConstant withdrawalRequestStatusConstant);
    Page<SavingsWithdrawalRequestEntity> getSavingsWithdrawal(AppUserEntity appUserEntity, WithdrawalRequestStatusConstant withdrawalRequestStatusConstant, LocalDate fromDate, LocalDate toDate, int pageNumber, int pageSize);
}
