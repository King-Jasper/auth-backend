package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.TierLevelEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TierLevelTypeConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 04 Feb, 2020
 */
public interface TierLevelRepository extends JpaRepository<TierLevelEntity, Long> {
    List<TierLevelEntity> getAllByRecordStatus(RecordStatusConstant statusConstant);
    Optional<TierLevelEntity> findFirstByTierId(String tierId);
    Optional<TierLevelEntity> findFirstByLevel(TierLevelTypeConstant levelTypeConstant);
}
