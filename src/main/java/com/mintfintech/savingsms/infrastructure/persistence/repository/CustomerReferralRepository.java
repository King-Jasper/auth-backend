package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.CustomerReferralEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 15 Dec, 2020
 */
public interface CustomerReferralRepository extends JpaRepository<CustomerReferralEntity, Long> {
    boolean existsByReferred(MintAccountEntity referred);
    boolean existsByReferrerAndReferred(MintAccountEntity referral, MintAccountEntity referred);
    Optional<CustomerReferralEntity> findFirstByReferredAndReferredRewardedFalse(MintAccountEntity referred);
    long countAllByReferrer(MintAccountEntity mintAccountEntity);
    Optional<CustomerReferralEntity> findFirstByReferredAndReferralCodeIgnoreCase(MintAccountEntity referred, String referralCode);


    List<CustomerReferralEntity> getAllByReferrerAndDateCreatedBetweenOrderByDateCreatedDesc(MintAccountEntity referral,
                                                                       LocalDateTime start,
                                                                       LocalDateTime end,
                                                                       Pageable pageable);
}
