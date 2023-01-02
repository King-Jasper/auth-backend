package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsInterestEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
public interface SavingsInterestRepository extends JpaRepository<SavingsInterestEntity, Long> {
    Optional<SavingsInterestEntity> findFirstBySavingsGoalOrderByDateCreatedDesc(SavingsGoalEntity savingsGoalEntity);
    Optional<SavingsInterestEntity> findFirstBySavingsGoalOrderByDateCreatedAsc(SavingsGoalEntity savingsGoalEntity);

    @Query("select sum(si.interest) from SavingsInterestEntity si where si.savingsGoal =:goal")
    Optional<BigDecimal> sumSavingsInterest(@Param("goal") SavingsGoalEntity savingsGoalEntity);

    long countAllBySavingsGoal(SavingsGoalEntity savingsGoalEntity);

    long countAllBySavingsGoalAndDateCreatedBetween(SavingsGoalEntity savingsGoalEntity, LocalDateTime fromTime, LocalDateTime toTime);

    Page<SavingsInterestEntity> getAllByRecordStatusAndSavingsGoalOrderByDateCreatedDesc(RecordStatusConstant status, SavingsGoalEntity savingsGoalEntity, Pageable pageable);

    List<SavingsInterestEntity> getAllByRecordStatusAndSavingsGoalOrderByDateCreated(RecordStatusConstant status, SavingsGoalEntity savingsGoalEntity);


    @Query(value = "select s from SavingsInterestEntity s where s.savingsGoal = ?1 and " +
            " to_char(s.dateCreated, 'YYYY-MM-DD') =:recordDate and " +
            "s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE")
    Optional<SavingsInterestEntity> getInterestOnDate(SavingsGoalEntity savingsGoalEntity, @Param("recordDate") String recordDate);

}
