package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
public interface SavingsGoalRepository extends JpaRepository<SavingsGoalEntity, Long> {
    List<SavingsGoalEntity> getAllByMintAccountAndRecordStatusOrderByDateCreatedDesc(MintAccountEntity accountEntity, RecordStatusConstant recordStatusConstant);
    Optional<SavingsGoalEntity> findFirstByMintAccountAndSavingsPlanAndRecordStatusAndNameIgnoreCase(MintAccountEntity accountEntity,
                                                                                                     SavingsPlanEntity planEntity,
                                                                                                     RecordStatusConstant statusConstant,
                                                                                                     String name);
    long countAllByRecordStatusAndMintAccountAndSavingsPlan(RecordStatusConstant statusConstant, MintAccountEntity accountEntity, SavingsPlanEntity planEntity);

    Optional<SavingsGoalEntity> findFirstByMintAccountAndGoalId(MintAccountEntity accountEntity, String goalId);

    @Query("select count(s) from SavingsGoalEntity s where s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE " +
            "and s.creationSource = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalCreationSourceConstant.CUSTOMER and " +
            "s.goalStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.ACTIVE")
    long countEligibleInterestSavingsGoal();

    @Query("select s from SavingsGoalEntity s where s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE " +
            "and s.creationSource = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalCreationSourceConstant.CUSTOMER and " +
            "s.goalStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.ACTIVE")
    Page<SavingsGoalEntity> getEligibleInterestSavingsGoal(Pageable pageable);

}
