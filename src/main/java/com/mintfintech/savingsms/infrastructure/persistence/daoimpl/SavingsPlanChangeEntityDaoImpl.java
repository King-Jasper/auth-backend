package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.SavingsPlanChangeEntityDao;
import com.mintfintech.savingsms.domain.entities.SavingsPlanChangeEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SavingsPlanChangeRepository;

import javax.inject.Named;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
@Named
public class SavingsPlanChangeEntityDaoImpl implements SavingsPlanChangeEntityDao {

    private SavingsPlanChangeRepository repository;
    public SavingsPlanChangeEntityDaoImpl(SavingsPlanChangeRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SavingsPlanChangeEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public SavingsPlanChangeEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. SavingsPlanChangeEntity with id: "+aLong));
    }

    @Override
    public SavingsPlanChangeEntity saveRecord(SavingsPlanChangeEntity record) {
        return repository.save(record);
    }
}
