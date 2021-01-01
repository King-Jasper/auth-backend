package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.CustomerReferralEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 15 Dec, 2020
 */
public interface CustomerReferralEntityDao extends CrudDao<CustomerReferralEntity, Long> {
    boolean recordExistForAccounts(MintAccountEntity referral, MintAccountEntity referred);
    Optional<CustomerReferralEntity> findUnprocessedReferredAccountReward(MintAccountEntity referred);
    long totalReferralRecordsForAccount(MintAccountEntity referral);
}
