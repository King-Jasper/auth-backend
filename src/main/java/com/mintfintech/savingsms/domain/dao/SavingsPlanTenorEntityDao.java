package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanTenorEntity;
import com.mintfintech.savingsms.domain.entities.enums.SavingsDurationTypeConstant;

import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
public interface SavingsPlanTenorEntityDao extends CrudDao<SavingsPlanTenorEntity, Long>{
    long countSavingsPlanTenor();
    List<SavingsPlanTenorEntity> getTenorList();
    SavingsPlanTenorEntity getSavingPlanTenor(SavingsPlanEntity planEntity, int duration, SavingsDurationTypeConstant durationTypeConstant);
    Optional<SavingsPlanTenorEntity> findSavingPlanTenor(SavingsPlanEntity planEntity, int duration);
    Optional<SavingsPlanTenorEntity> findSavingPlanTenor(int minimumDuration, int maximumDuration);
    SavingsPlanTenorEntity getLeastDurationOnSavingsPlan(SavingsPlanEntity planEntity);
    List<SavingsPlanTenorEntity> getTenorListByPlan(SavingsPlanEntity planEntity);
    Optional<SavingsPlanTenorEntity> findSavingsPlanTenorForDuration(int duration);
    int getMaximumSavingsDuration();
   // Optional<SavingsPlanTenorEntity> findTenorByPlanAndId(SavingsPlanEntity planEntity, Long tenorId);
}
