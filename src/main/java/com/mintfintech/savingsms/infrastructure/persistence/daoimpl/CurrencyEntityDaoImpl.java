package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.CurrencyEntityDao;
import com.mintfintech.savingsms.domain.entities.CurrencyEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.CurrencyRepository;

import javax.inject.Named;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
@Named
public class CurrencyEntityDaoImpl implements CurrencyEntityDao {

    private final CurrencyRepository repository;
    public CurrencyEntityDaoImpl(CurrencyRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CurrencyEntity> findByCode(String code) {
        return repository.findFirstByCodeIgnoreCase(code);
    }

    @Override
    public long countRecords() {
        return repository.count();
    }

    @Override
    public CurrencyEntity getByCode(String code) {
        return findByCode(code).orElseThrow(() -> new RuntimeException("Not found: Currency with code: "+code));
    }

    @Override
    public Optional<CurrencyEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public CurrencyEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. CurrencyEntity with id: "+aLong));
    }

    @Override
    public CurrencyEntity saveRecord(CurrencyEntity record) {
        return repository.save(record);
    }
}
