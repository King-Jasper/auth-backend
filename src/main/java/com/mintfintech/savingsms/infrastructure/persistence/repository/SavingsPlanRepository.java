package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsPlanTypeConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
public interface SavingsPlanRepository extends JpaRepository<SavingsPlanEntity, Long> {
    Optional<SavingsPlanEntity> findFirstByPlanId(String planId);
    List<SavingsPlanEntity> getAllByRecordStatus(RecordStatusConstant statusConstant);
    SavingsPlanEntity getFirstByRecordStatusAndPlanName(RecordStatusConstant statusConstant, SavingsPlanTypeConstant planTypeConstant);
}
