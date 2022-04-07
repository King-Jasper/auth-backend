package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant;
import com.mintfintech.savingsms.domain.models.reports.SavingsMaturityStat;
import org.apache.kafka.common.record.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
public interface SavingsGoalRepository extends JpaRepository<SavingsGoalEntity, Long>, JpaSpecificationExecutor<SavingsGoalEntity> {



    @Query("select s from SavingsGoalEntity s where s.mintAccount = ?1 and s.recordStatus = ?2 and" +
            " (s.goalStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.ACTIVE or " +
            " s.goalStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.MATURED)")
    List<SavingsGoalEntity> getCurrentAccountGoals(MintAccountEntity accountEntity, RecordStatusConstant recordStatusConstant);


    Optional<SavingsGoalEntity> findFirstByMintAccountAndSavingsPlanAndGoalStatusAndRecordStatusAndNameIgnoreCase(MintAccountEntity accountEntity,
                                                                                                     SavingsPlanEntity planEntity,
                                                                                                     SavingsGoalStatusConstant goalStatus,
                                                                                                     RecordStatusConstant recordStatus,
                                                                                                     String name);

    @Query("select count(s) from SavingsGoalEntity s where s.mintAccount = ?2 and s.recordStatus = ?1 and s.savingsPlan = ?3 and " +
            " s.savingsGoalType = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant.CUSTOMER_SAVINGS and" +
            " (s.goalStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.ACTIVE or " +
            " s.goalStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.MATURED)")
    long countActiveCustomerCreatedGoalsOnAccountAndPlan(RecordStatusConstant statusConstant, MintAccountEntity accountEntity,
                                            SavingsPlanEntity planEntity, SavingsGoalTypeConstant goalTypeConstant);

    @Query("select count(s) from SavingsGoalEntity s where s.mintAccount = ?2 and s.recordStatus = ?1 and s.savingsGoalType = ?3 and " +
            " (s.goalStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.ACTIVE or " +
            " s.goalStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.MATURED)")
    long countActiveCustomerCreatedGoalsOnAccount(RecordStatusConstant statusConstant, MintAccountEntity accountEntity,
                                                  SavingsGoalTypeConstant goalTypeConstant);

    /*long countAllByRecordStatusAndMintAccountAndSavingsGoalType(RecordStatusConstant statusConstant,
                                                                MintAccountEntity accountEntity,
                                                                SavingsGoalTypeConstant goalTypeConstant);*/

    Optional<SavingsGoalEntity> findFirstByMintAccountAndGoalIdAndRecordStatus(MintAccountEntity accountEntity, String goalId, RecordStatusConstant status);

    Optional<SavingsGoalEntity> findFirstByGoalIdAndRecordStatus(String goalId, RecordStatusConstant status);

    Optional<SavingsGoalEntity> findFirstByMintAccountAndSavingsGoalTypeAndRecordStatusOrderByDateCreatedDesc(MintAccountEntity mintAccountEntity,
                                                                                        SavingsGoalTypeConstant goalTypeConstant,
                                                                                        RecordStatusConstant status);

    Optional<SavingsGoalEntity> findFirstByMintAccountAndSavingsGoalTypeOrderByDateCreatedDesc(MintAccountEntity mintAccountEntity, SavingsGoalTypeConstant goalTypeConstant);

    @Query("select count(s) from SavingsGoalEntity s where s.savingsBalance > 0.0 and s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE " +
            "and (s.savingsGoalType = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant.CUSTOMER_SAVINGS or " +
            " s.savingsGoalType = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant.ROUND_UP_SAVINGS) and " +
            " s.goalStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.ACTIVE")
    long countEligibleInterestSavingsGoal();

    @Query("select s from SavingsGoalEntity s where s.savingsBalance > 0.0 and s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE " +
            "and (s.savingsGoalType = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant.CUSTOMER_SAVINGS or " +
            "s.savingsGoalType = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant.ROUND_UP_SAVINGS or " +
            "s.savingsGoalType = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant.SPEND_AND_SAVE) " +
            " and s.goalStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.ACTIVE")
    Page<SavingsGoalEntity> getEligibleInterestSavingsGoal(Pageable pageable);


   /* @Query("select s from SavingsGoalEntity s where s.savingsBalance > 0.0 and s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE " +
            " and s.goalStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.ACTIVE")
    Page<SavingsGoalEntity> getEligibleInterestSavingsGoal(Pageable pageable); */

    @Query(value = "select s from SavingsGoalEntity s where s.goalStatus = :status and s.autoSave = true and" +
            " s.nextAutoSaveDate is not null and to_char(s.nextAutoSaveDate, 'YYYY-MM-DD HH24') =:dateWithHour24 and " +
            "s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE")
    List<SavingsGoalEntity> getSavingsGoalWithMatchingSavingHour(@Param("status") SavingsGoalStatusConstant status, @Param("dateWithHour24") String dateWithHour24);


    @Query(value = "select s from SavingsGoalEntity s where s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE" +
            " and s.maturityDate is not null and (s.savingsGoalType = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant.CUSTOMER_SAVINGS or " +
            " s.savingsGoalType = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant.ROUND_UP_SAVINGS)" +
            "and s.goalStatus  =:status and s.maturityDate between :fromTime and :toTime")
    Page<SavingsGoalEntity> getSavingsGoalWithMaturityPeriod(@Param("status") SavingsGoalStatusConstant status,
                                                             @Param("fromTime") LocalDateTime fromTime,
                                                             @Param("toTime") LocalDateTime toTime, Pageable pageable);


    @Query(value = "select new com.mintfintech.savingsms.domain.models.reports.SavingsMaturityStat(DAY(s.maturityDate), MONTH(s.maturityDate), count(s), sum(s.accruedInterest), sum(s.savingsBalance)) " +
            "from SavingsGoalEntity s where s.maturityDate is not null and s.maturityDate between :startDate and :endDate and" +
            " s.creationSource = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalCreationSourceConstant.CUSTOMER and " +
            " s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE " +
            "group by DAY(s.maturityDate), MONTH(s.maturityDate)")
    List<SavingsMaturityStat> getSavingsMaturityStatistics(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);


    @Query("select s from SavingsGoalEntity s where s.creationSource = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalCreationSourceConstant.CUSTOMER and " +
            "s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and " +
            "s.goalStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.ACTIVE")
    List<SavingsGoalEntity> getSavings();

}
