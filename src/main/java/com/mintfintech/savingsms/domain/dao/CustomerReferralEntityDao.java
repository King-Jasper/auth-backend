package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.CustomerReferralEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.models.reports.ReferralRewardStat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 15 Dec, 2020
 */
public interface CustomerReferralEntityDao extends CrudDao<CustomerReferralEntity, Long> {
    boolean recordExistForReferredAccount(MintAccountEntity referred);
    boolean recordExistForAccounts(MintAccountEntity referral, MintAccountEntity referred);
    Optional<CustomerReferralEntity> findUnprocessedReferredAccountReward(MintAccountEntity referred);
    long totalReferralRecordsForAccount(MintAccountEntity referral);
    long countUnprocessedReferralRecordsForAccount(MintAccountEntity referral, LocalDateTime fromDate);
    List<CustomerReferralEntity> getUnprocessedReferralRecordsForAccount(MintAccountEntity referral, LocalDateTime fromDate, int size);
    List<ReferralRewardStat> getReferralRewardStatOnAccount(MintAccountEntity accountEntity);
    List<CustomerReferralEntity> getUnprocessedRecordByReferral(MintAccountEntity referral, LocalDateTime start, LocalDateTime end, int size);
    List<CustomerReferralEntity> getUnprocessedRecordByReferral(LocalDateTime start, LocalDateTime end, int size, BigDecimal savingsMinimumBalance);
    Optional<CustomerReferralEntity> findRecordByReferralCodeAndReferredAccount(String code, MintAccountEntity referredAccount);
    List<CustomerReferralEntity> getProcessedReferralsByReferrer(MintAccountEntity mintAccount, LocalDateTime start, LocalDateTime end);
}
