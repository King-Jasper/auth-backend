package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanTenorEntity;
import com.mintfintech.savingsms.domain.entities.enums.SavingsDurationTypeConstant;

import java.util.List;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
public interface SavingsPlanTenorEntityDao extends CrudDao<SavingsPlanTenorEntity, Long>{
    long countSavingsPlanTenor();
    List<SavingsPlanTenorEntity> getTenorList();
    SavingsPlanTenorEntity getSavingPlanTenor(SavingsPlanEntity planEntity, int duration, SavingsDurationTypeConstant durationTypeConstant);
    SavingsPlanTenorEntity getLeastDurationOnSavingsPlan(SavingsPlanEntity planEntity);
    List<SavingsPlanTenorEntity> getTenorListByPlan(SavingsPlanEntity planEntity);
   // Optional<SavingsPlanTenorEntity> findTenorByPlanAndId(SavingsPlanEntity planEntity, Long tenorId);
}
