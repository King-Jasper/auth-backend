package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.RoundUpSavingsSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
public interface RoundUpSavingsSettingRepository extends JpaRepository<RoundUpSavingsSettingEntity, Long> {
    Optional<RoundUpSavingsSettingEntity> findTopByCreatorAndEnabledTrue(AppUserEntity appUserEntity);
    Optional<RoundUpSavingsSettingEntity> findTopByCreator(AppUserEntity appUserEntity);
    Optional<RoundUpSavingsSettingEntity> findTopByAccount(MintAccountEntity mintAccountEntity);
}
