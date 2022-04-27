package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CorporateUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Sun, 18 Jul, 2021
 */
public interface CorporateUserRepository extends JpaRepository<CorporateUserEntity, Long> {
    Optional<CorporateUserEntity> findTopByAppUser_UserIdAndCorporateAccount_AccountId(String userId, String accountId);
    Optional<CorporateUserEntity> findTopByAppUserAndCorporateAccount(AppUserEntity user, MintAccountEntity corporateAccount);
    List<CorporateUserEntity> findAllByCorporateAccount(MintAccountEntity mintAccount);
    Optional<CorporateUserEntity> findTopByAppUserAndRecordStatus(AppUserEntity appUser, RecordStatusConstant statusConstant);
}
