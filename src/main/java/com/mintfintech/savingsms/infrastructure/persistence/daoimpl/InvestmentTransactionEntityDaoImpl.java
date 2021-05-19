package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.InvestmentTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.InvestmentTransactionEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.InvestmentTransactionRepository;

import javax.inject.Named;
import java.util.Optional;

@Named
public class InvestmentTransactionEntityDaoImpl implements InvestmentTransactionEntityDao {

    private final InvestmentTransactionRepository repository;

    public InvestmentTransactionEntityDaoImpl(InvestmentTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<InvestmentTransactionEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public InvestmentTransactionEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. InvestmentTransactionEntity with id: " + aLong));
    }

    @Override
    public InvestmentTransactionEntity saveRecord(InvestmentTransactionEntity record) {
        return repository.save(record);
    }
}
