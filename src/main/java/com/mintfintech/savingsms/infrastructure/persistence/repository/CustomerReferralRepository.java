package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.CustomerReferralEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Created by jnwanya on
 * Tue, 15 Dec, 2020
 */
public interface CustomerReferralRepository extends JpaRepository<CustomerReferralEntity, Long> {
    boolean existsByReferrerAndReferred(MintAccountEntity referral, MintAccountEntity referred);
}
