package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.LoanApprovalEntityDao;
import com.mintfintech.savingsms.domain.entities.LoanApprovalEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.LoanApprovalRepository;

import javax.inject.Named;
import java.util.List;
import java.util.Optional;

@Named
public class LoanApprovalEntityDaoImpl implements LoanApprovalEntityDao {

    private final LoanApprovalRepository repository;

    public LoanApprovalEntityDaoImpl(LoanApprovalRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<LoanApprovalEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public LoanApprovalEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. LoanApprovalEntity with Id :" + aLong));
    }

    @Override
    public LoanApprovalEntity saveRecord(LoanApprovalEntity record) {
        return repository.save(record);
    }

    @Override
    public List<LoanApprovalEntity> getPendingMintToSuspenseTransaction(String responseCode) {
        return repository.getPendingMintToSuspenseTransaction(responseCode);
    }

    @Override
    public List<LoanApprovalEntity> getPendingInterestToSuspenseTransaction(String responseCode) {
        return repository.getPendingInterestToSuspenseTransaction(responseCode);
    }

    @Override
    public List<LoanApprovalEntity> getPendingSuspenseToCustomerTransaction() {
        return repository.getPendingSuspenseToCustomerTransaction();
    }
}
