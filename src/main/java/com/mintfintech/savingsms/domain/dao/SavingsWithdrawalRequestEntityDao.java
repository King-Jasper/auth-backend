package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsWithdrawalRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.WithdrawalRequestStatusConstant;
import com.mintfintech.savingsms.domain.models.SavingsSearchDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by jnwanya on
 * Tue, 07 Apr, 2020
 */
public interface SavingsWithdrawalRequestEntityDao extends CrudDao<SavingsWithdrawalRequestEntity, Long> {
    Page<SavingsWithdrawalRequestEntity> getSavingsWithdrawalReport(SavingsSearchDTO searchDTO, int pageIndex, int recordSize);

    SavingsWithdrawalRequestEntity saveAndFlush(SavingsWithdrawalRequestEntity savingsWithdrawalRequestEntity);
    String generateTransactionReference();
    long countWithdrawalRequestWithinPeriod(SavingsGoalEntity savingsGoal, LocalDateTime fromTime, LocalDateTime toTime);
    List<SavingsWithdrawalRequestEntity> getSavingsWithdrawalByStatus(WithdrawalRequestStatusConstant withdrawalRequestStatusConstant);

}
