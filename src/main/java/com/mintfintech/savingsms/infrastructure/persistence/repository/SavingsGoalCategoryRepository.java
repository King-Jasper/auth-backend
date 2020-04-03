package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.SavingsGoalCategoryEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Wed, 01 Apr, 2020
 */
public interface SavingsGoalCategoryRepository extends JpaRepository<SavingsGoalCategoryEntity, Long> {
    List<SavingsGoalCategoryEntity> getAllByRecordStatus(RecordStatusConstant statusConstant);
    long countAllByRecordStatus(RecordStatusConstant statusConstant);
    Optional<SavingsGoalCategoryEntity> findFirstByCode(String code);
}
