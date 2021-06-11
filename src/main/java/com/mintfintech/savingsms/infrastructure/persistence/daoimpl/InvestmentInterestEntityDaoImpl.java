package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.InvestmentInterestEntityDao;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentInterestEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.InvestmentInterestRepository;

import javax.inject.Named;
import java.math.BigDecimal;
import java.util.Optional;

@Named
public class InvestmentInterestEntityDaoImpl implements InvestmentInterestEntityDao {

    private final InvestmentInterestRepository repository;

    public InvestmentInterestEntityDaoImpl(InvestmentInterestRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<InvestmentInterestEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public InvestmentInterestEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. InvestmentInterestEntity with Id: " + aLong));
    }

    @Override
    public InvestmentInterestEntity saveRecord(InvestmentInterestEntity record) {
        return repository.save(record);
    }

    @Override
    public BigDecimal getTotalInterestAmountOnInvestment(InvestmentEntity investmentEntity) {
        return repository.sumInvestmentInterest(investmentEntity).orElse(BigDecimal.ZERO);
    }
}
