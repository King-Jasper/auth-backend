package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.CorporateUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Sun, 18 Jul, 2021
 */
public interface CorporateUserRepository extends JpaRepository<CorporateUserEntity, Long> {
    Optional<CorporateUserEntity> findTopByAppUser_UserIdAndCorporateAccount_AccountId(String userId, String accountId);
}
