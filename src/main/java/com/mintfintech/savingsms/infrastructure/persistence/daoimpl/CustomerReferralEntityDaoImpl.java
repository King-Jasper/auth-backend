package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.CustomerReferralEntityDao;
import com.mintfintech.savingsms.domain.entities.CustomerReferralEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.CustomerReferralRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.inject.Named;
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
    public Optional<CustomerReferralEntity> findUnprocessedReferredAccountReward(MintAccountEntity referred) {
        return repository.findFirstByReferredAndReferredRewardedFalse(referred);
    }

    @Override
    public long totalReferralRecordsForAccount(MintAccountEntity referral) {
        return repository.countAllByReferrer(referral);
    }

    @Override
    public List<CustomerReferralEntity> getByReferral(MintAccountEntity referral, LocalDateTime start, LocalDateTime end) {
        Pageable pageable = PageRequest.of(0, 15);
        return repository.getAllByReferrerAndDateCreatedBetween(referral, start, end, pageable);
    }
}
