package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.models.PagedResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
public interface SavingsGoalEntityDao extends CrudDao<SavingsGoalEntity, Long> {
    String generateSavingGoalId();
    List<SavingsGoalEntity>  getAccountSavingGoals(MintAccountEntity accountEntity);
    Optional<SavingsGoalEntity> findSavingGoalByAccountAndGoalId(MintAccountEntity accountEntity, String goalId);
    Optional<SavingsGoalEntity> findGoalByNameAndPlanAndAccount(String name, SavingsPlanEntity planEntity, MintAccountEntity accountEntity);
    long countAccountSavingsGoalsOnPlan(MintAccountEntity mintAccountEntity, SavingsPlanEntity planEntity);
    long countAccountSavingsGoals(MintAccountEntity mintAccountEntity);
    long countEligibleInterestSavingsGoal();
    PagedResponse<SavingsGoalEntity> getPagedEligibleInterestSavingsGoal(int pageIndex, int recordSize);
    List<SavingsGoalEntity> getSavingGoalWithAutoSaveTime(LocalDateTime autoSaveTime);
}
