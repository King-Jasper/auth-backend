package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.InvestmentTenorEntityDao;
import com.mintfintech.savingsms.domain.entities.InvestmentTenorEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.InvestmentTenorRepository;

import javax.inject.Named;
import java.util.List;
import java.util.Optional;

@Named
public class InvestmentTenorEntityDaoImpl implements InvestmentTenorEntityDao {

    private final InvestmentTenorRepository repository;

    public InvestmentTenorEntityDaoImpl(InvestmentTenorRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<InvestmentTenorEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public InvestmentTenorEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. InvestmentTenorEntity with id: "+aLong));
    }

    @Override
    public InvestmentTenorEntity saveRecord(InvestmentTenorEntity record) {
        return repository.save(record);
    }

    @Override
    public Optional<InvestmentTenorEntity> findInvestmentTenor(int minimumDuration, int maximumDuration) {
        return repository.findFirstByMinimumDurationAndMaximumDurationAndRecordStatus(minimumDuration, maximumDuration, RecordStatusConstant.ACTIVE);
    }

    @Override
    public List<InvestmentTenorEntity> getTenorList() {
        return repository.getAllByRecordStatus(RecordStatusConstant.ACTIVE);
    }

    @Override
    public Optional<InvestmentTenorEntity> findInvestmentTenorForDuration(int duration, RecordStatusConstant status) {
        return repository.findInvestmentTenorForDuration(duration, status);
    }
}
