package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.TierLevelEntity;
import com.mintfintech.savingsms.domain.entities.enums.TierLevelTypeConstant;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
public interface TierLevelEntityDao extends CrudDao<TierLevelEntity, Long> {
    Optional<TierLevelEntity> findByTierId(String tierId);
    Optional<TierLevelEntity> findByTierLevelType(TierLevelTypeConstant tierLevelType);
    TierLevelEntity getByTierLevelType(TierLevelTypeConstant tierLevelType);
    long countRecords();
}
