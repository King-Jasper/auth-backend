package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.RoundUpSavingsSettingEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
public interface RoundUpSavingsSettingEntityDao extends CrudDao<RoundUpSavingsSettingEntity, Long> {
    Optional<RoundUpSavingsSettingEntity> findRoundUpSavingsByUser(AppUserEntity user);
    Optional<RoundUpSavingsSettingEntity> findRoundUpSavingsByAccount(MintAccountEntity mintAccount);
    Optional<RoundUpSavingsSettingEntity> findActiveRoundUpSavingsByUser(AppUserEntity user);
    List<RoundUpSavingsSettingEntity> getDeactivateSavingsWithZeroBalance(LocalDateTime deactivatedBeforeTime, int size);
}
