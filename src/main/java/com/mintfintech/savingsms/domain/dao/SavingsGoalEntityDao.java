package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
import com.mintfintech.savingsms.domain.models.PagedResponse;
import com.mintfintech.savingsms.domain.models.SavingsSearchDTO;
import com.mintfintech.savingsms.domain.models.reports.SavingsMaturityStat;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
public interface SavingsGoalEntityDao extends CrudDao<SavingsGoalEntity, Long> {
    String generateSavingGoalId();
    Optional<SavingsGoalEntity> findFirstSavingsByType(MintAccountEntity accountEntity, SavingsGoalTypeConstant savingsGoalType);
    Optional<SavingsGoalEntity> findFirstSavingsByTypeIgnoreStatus(MintAccountEntity accountEntity, SavingsGoalTypeConstant savingsGoalType);
    List<SavingsGoalEntity>  getAccountSavingGoals(MintAccountEntity accountEntity);
    Optional<SavingsGoalEntity> findSavingGoalByAccountAndGoalId(MintAccountEntity accountEntity, String goalId);
    Optional<SavingsGoalEntity> findSavingGoalByGoalId(String goalId);
    Optional<SavingsGoalEntity> findGoalByNameAndPlanAndAccount(String name, SavingsPlanEntity planEntity, MintAccountEntity accountEntity);
    long countUserCreatedSavingsGoalsOnPlan(MintAccountEntity mintAccountEntity, SavingsPlanEntity planEntity);
    long countUserCreatedAccountSavingsGoals(MintAccountEntity mintAccountEntity);
    long countEligibleInterestSavingsGoal();
    PagedResponse<SavingsGoalEntity> getPagedEligibleInterestSavingsGoal(int pageIndex, int recordSize);
    List<SavingsGoalEntity> getSavingGoalWithAutoSaveTime(LocalDateTime autoSaveTime);
    PagedResponse<SavingsGoalEntity> getPagedSavingsGoalsWithMaturityDateWithinPeriod(LocalDateTime fromTime, LocalDateTime toTime, int pageIndex, int recordSize);

    Page<SavingsGoalEntity> searchSavingsGoal(SavingsSearchDTO savingsSearchDTO, int pageIndex, int recordSize);
    BigDecimal sumSearchedSavingsGoal(SavingsSearchDTO savingsSearchDTO);

    List<SavingsMaturityStat> savingsMaturityStatisticsList(LocalDateTime startDate, LocalDateTime endDate);

    void deleteSavings(SavingsGoalEntity savingsGoalEntity);
}
