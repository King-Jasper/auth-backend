package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.LoanRepaymentEntityDao;
import com.mintfintech.savingsms.domain.entities.LoanRepaymentEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.LoanRepaymentRepository;

import javax.inject.Named;
import java.util.List;
import java.util.Optional;

@Named
public class LoanRepaymentEntityDaoImpl implements LoanRepaymentEntityDao {

    private final LoanRepaymentRepository repository;

    public LoanRepaymentEntityDaoImpl(LoanRepaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<LoanRepaymentEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public LoanRepaymentEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. LoanRepaymentEntity with Id :" + aLong));
    }

    @Override
    public LoanRepaymentEntity saveRecord(LoanRepaymentEntity record) {
        return repository.save(record);
    }

    @Override
    public List<LoanRepaymentEntity> getPendingRecoveryToMintTransaction() {
        return repository.getPendingRecoveryToMintTransaction( );
    }

    @Override
    public List<LoanRepaymentEntity> getPendingSuspenseToIncomeTransaction() {
        return repository.getPendingSuspenseToIncomeTransaction();
    }

    @Override
    public LoanRepaymentEntity saveAndFlush(LoanRepaymentEntity loanRepaymentEntity) {
        return repository.saveAndFlush(loanRepaymentEntity);
    }
}
