package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.SettingsEntity;
import com.mintfintech.savingsms.domain.entities.enums.SettingsNameTypeConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Wed, 16 Dec, 2020
 */
public interface SettingsRepository extends JpaRepository<SettingsEntity, Long> {
    Optional<SettingsEntity> findFirstByName(SettingsNameTypeConstant nameTypeConstant);
}
