package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.LoanTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.LoanTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.LoanTransactionTypeConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.LoanTransactionRepository;

import javax.inject.Named;
import java.util.List;
import java.util.Optional;

@Named
public class LoanTransactionEntityDaoImpl implements LoanTransactionEntityDao {

    private final LoanTransactionRepository repository;

    public LoanTransactionEntityDaoImpl(LoanTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<LoanTransactionEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public LoanTransactionEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. LoanRepaymentTransactionEntity with Id :" + aLong));
    }

    @Override
    public LoanTransactionEntity saveRecord(LoanTransactionEntity record) {
        return repository.save(record);
    }

    @Override
    public List<LoanTransactionEntity> getLoanTransactions(LoanRequestEntity loanRequestEntity) {
        return repository.getAllByRecordStatusAndLoanRequest(loanRequestEntity);
    }

    @Override
    public List<LoanTransactionEntity> getDebitLoanTransactions(LoanRequestEntity loanRequestEntity) {
        return repository.getDebitTransactionByLoanRequest(loanRequestEntity);
    }


}
