package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.CustomerReferralEntityDao;
import com.mintfintech.savingsms.domain.entities.CustomerReferralEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.models.reports.ReferralRewardStat;
import com.mintfintech.savingsms.infrastructure.persistence.repository.CustomerReferralRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.inject.Named;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 15 Dec, 2020
 */
@Named
public class CustomerReferralEntityDaoImpl extends CrudDaoImpl<CustomerReferralEntity, Long> implements CustomerReferralEntityDao {

    private final CustomerReferralRepository repository;
    public CustomerReferralEntityDaoImpl(CustomerReferralRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public boolean recordExistForAccounts(MintAccountEntity referral, MintAccountEntity referred) {
        return repository.existsByReferrerAndReferred(referral, referred);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public boolean recordExistForReferredAccount(MintAccountEntity referred) {
        return repository.existsByReferred(referred);
    }

    @Override
    public Optional<CustomerReferralEntity> findUnprocessedReferredAccountReward(MintAccountEntity referred) {
        return repository.findFirstByReferredAndReferredRewardedFalse(referred);
    }

    @Override
    public long totalReferralRecordsForAccount(MintAccountEntity referral) {
        return repository.countAllByReferrer(referral);
    }

    @Override
    public List<ReferralRewardStat> getReferralRewardStatOnAccount(MintAccountEntity accountEntity) {
        return repository.getReferralStatisticsForAccount(accountEntity);
    }

    @Override
    public List<CustomerReferralEntity> getUnprocessedRecordByReferral(MintAccountEntity referral, LocalDateTime start, LocalDateTime end, int size) {
        Pageable pageable = PageRequest.of(0, size);
        return repository.getAllByReferrerAndReferrerRewardedAndDateCreatedBetweenOrderByDateCreatedDesc(referral, false, start, end, pageable);
    }

    @Override
    public List<CustomerReferralEntity> getUnprocessedRecordByReferral(LocalDateTime start, LocalDateTime end, int size, BigDecimal savingsMinimumBalance) {
        Pageable pageable = PageRequest.of(0, size);
        return repository.getUnprocessedReferrals(start, end, savingsMinimumBalance, pageable);
    }

    @Override
    public Optional<CustomerReferralEntity> findRecordByReferralCodeAndReferredAccount(String code, MintAccountEntity referredAccount) {
        return repository.findFirstByReferredAndReferralCodeIgnoreCase(referredAccount, code.toUpperCase());
    }

    @Override
    public List<CustomerReferralEntity> getProcessedReferralsByReferrer(MintAccountEntity referrer, LocalDateTime start, LocalDateTime end) {
        return repository.getAllByReferrerAndDateCreatedBetween(referrer, start, end);
    }
}
