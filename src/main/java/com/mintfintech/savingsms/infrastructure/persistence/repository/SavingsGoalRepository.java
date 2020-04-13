package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    long countAllByRecordStatusAndMintAccountAndSavingsPlanAndSavingsGoalType(RecordStatusConstant statusConstant,
                                                                              MintAccountEntity accountEntity,
                                                                              SavingsPlanEntity planEntity,
                                                                              SavingsGoalTypeConstant goalTypeConstant);

    long countAllByRecordStatusAndMintAccountAndSavingsGoalType(RecordStatusConstant statusConstant, MintAccountEntity accountEntity,
                                                                SavingsGoalTypeConstant goalTypeConstant);

    Optional<SavingsGoalEntity> findFirstByMintAccountAndGoalId(MintAccountEntity accountEntity, String goalId);

    @Query("select count(s) from SavingsGoalEntity s where s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE " +
            "and s.creationSource = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalCreationSourceConstant.CUSTOMER and " +
            "s.goalStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.ACTIVE")
    long countEligibleInterestSavingsGoal();

    @Query("select s from SavingsGoalEntity s where s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE " +
            "and s.creationSource = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalCreationSourceConstant.CUSTOMER and " +
            "s.goalStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.ACTIVE")
    Page<SavingsGoalEntity> getEligibleInterestSavingsGoal(Pageable pageable);


    @Query(value = "select s from SavingsGoalEntity s where s.goalStatus = :status and s.autoSave = true and" +
            " s.nextAutoSaveDate is not null and to_char(s.nextAutoSaveDate, 'YYYY-MM-DD HH24') =:dateWithHour24")
    List<SavingsGoalEntity> getSavingsGoalWithMatchingSavingHour(@Param("status") SavingsGoalStatusConstant status,
                                                                                    @Param("dateWithHour24") String dateWithHour24);

    @Query(value = "select s from SavingsGoalEntity s where s.goalStatus  =:status and " +
            "s.creationSource = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalCreationSourceConstant.CUSTOMER and" +
            " s.maturityDate between :fromTime and :toTime")
    Page<SavingsGoalEntity> getSavingsGoalWithMaturityPeriod(@Param("status") SavingsGoalStatusConstant status,
                                                             @Param("fromTime") LocalDateTime fromTime,
                                                             @Param("toTime") LocalDateTime toTime, Pageable pageable);

}
