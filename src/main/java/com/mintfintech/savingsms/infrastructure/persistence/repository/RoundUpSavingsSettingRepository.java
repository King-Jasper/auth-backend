package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.RoundUpSavingsSettingEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
public interface RoundUpSavingsSettingRepository extends JpaRepository<RoundUpSavingsSettingEntity, Long> {
    Optional<RoundUpSavingsSettingEntity> findTopByCreatorAndEnabledTrueAndRecordStatus(AppUserEntity appUserEntity, RecordStatusConstant statusConstant);
    Optional<RoundUpSavingsSettingEntity> findTopByCreatorAndRecordStatus(AppUserEntity appUserEntity, RecordStatusConstant statusConstant);
    Optional<RoundUpSavingsSettingEntity> findTopByAccountAndRecordStatus(MintAccountEntity mintAccountEntity, RecordStatusConstant statusConstant);

    @Query("select r from RoundUpSavingsSettingEntity r where r.enabled = false and r.roundUpSavings.savingsBalance = ?1 and " +
            "r.roundUpSavings.goalStatus = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant.ACTIVE and " +
            "r.dateDeactivated < ?2 order by r.dateCreated asc")
    List<RoundUpSavingsSettingEntity> getDeactivatedSavingsForDeletion(BigDecimal amount, LocalDateTime beforeTime, Pageable pageable);
}
