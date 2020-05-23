package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.enums.SavingsPlanTypeConstant;

import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
public interface SavingsPlanEntityDao extends CrudDao<SavingsPlanEntity, Long> {
    String generatePlanId();
    long countSavingPlans();
    List<SavingsPlanEntity> getSavingsPlans();
    SavingsPlanEntity getPlanByType(SavingsPlanTypeConstant planTypeConstant);
    Optional<SavingsPlanEntity> findBPlanByType(SavingsPlanTypeConstant planTypeConstant);
    Optional<SavingsPlanEntity> findPlanByPlanId(String planId);
}
