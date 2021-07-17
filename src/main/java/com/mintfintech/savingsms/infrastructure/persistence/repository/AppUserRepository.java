package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Wed, 29 Jan, 2020
 */
public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {
    Optional<AppUserEntity> findFirstByUserIdAndRecordStatus(String userId, RecordStatusConstant statusConstant);
    Optional<AppUserEntity> findFirstByPhoneNumber(String phoneNumber);
    Optional<AppUserEntity> findFirstByUserId(String userId);
    Optional<AppUserEntity> findFirstByPrimaryAccount(MintAccountEntity account);
    Optional<AppUserEntity> findTopByPhoneNumberAndRecordStatus(String phoneNumber, RecordStatusConstant statusConstant);

}
