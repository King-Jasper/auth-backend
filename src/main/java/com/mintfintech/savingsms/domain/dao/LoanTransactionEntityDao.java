package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.LoanTransactionEntity;

import java.util.List;

public interface LoanTransactionEntityDao extends CrudDao<LoanTransactionEntity, Long> {

    List<LoanTransactionEntity> getLoanTransactions(LoanRequestEntity loanRequestEntity);

    List<LoanTransactionEntity> getDebitLoanTransactions(LoanRequestEntity loanRequestEntity);
}
