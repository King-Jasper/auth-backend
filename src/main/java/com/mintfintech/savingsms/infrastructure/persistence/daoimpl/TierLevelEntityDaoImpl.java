package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.TierLevelEntityDao;
import com.mintfintech.savingsms.domain.entities.TierLevelEntity;
import com.mintfintech.savingsms.domain.entities.enums.TierLevelTypeConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.TierLevelRepository;

import javax.inject.Named;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
@Named
public class TierLevelEntityDaoImpl implements TierLevelEntityDao {

    private TierLevelRepository repository;
    public TierLevelEntityDaoImpl(TierLevelRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<TierLevelEntity> findByTierId(String tierId) {
        return repository.findFirstByTierId(tierId);
    }

    @Override
    public Optional<TierLevelEntity> findByTierLevelType(TierLevelTypeConstant tierLevelType) {
        return repository.findFirstByLevel(tierLevelType);
    }

    @Override
    public long countRecords() {
        return repository.count();
    }

    @Override
    public TierLevelEntity getByTierLevelType(TierLevelTypeConstant tierLevelType) {
        return findByTierLevelType(tierLevelType).orElseThrow(() -> new RuntimeException("Not found. TierLevelEntity with type: "+tierLevelType));
    }

    @Override
    public Optional<TierLevelEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public TierLevelEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException(" Not found. TierLevelEntity with id: "+aLong));
    }

    @Override
    public TierLevelEntity saveRecord(TierLevelEntity record) {
        return repository.save(record);
    }
}
