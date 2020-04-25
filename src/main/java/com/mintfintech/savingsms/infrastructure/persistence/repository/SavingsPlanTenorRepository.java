package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanTenorEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsDurationTypeConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
public interface SavingsPlanTenorRepository extends JpaRepository<SavingsPlanTenorEntity, Long> {
    List<SavingsPlanTenorEntity> getAllByRecordStatusAndSavingsPlan(RecordStatusConstant statusConstant, SavingsPlanEntity savingsPlanEntity);
    List<SavingsPlanTenorEntity> getAllByRecordStatus(RecordStatusConstant statusConstant);
    Optional<SavingsPlanTenorEntity> findFirstBySavingsPlanAndDurationTypeAndDurationAndRecordStatus(
            SavingsPlanEntity savingsPlanEntity, SavingsDurationTypeConstant durationTypeConstant, int duration, RecordStatusConstant statusConstant);
    SavingsPlanTenorEntity getFirstBySavingsPlanOrderByDurationAsc(SavingsPlanEntity savingsPlanEntity);

}
