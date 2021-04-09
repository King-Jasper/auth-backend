package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.LoanTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.LoanTransactionTypeConstant;

import java.util.List;

public interface LoanTransactionEntityDao extends CrudDao<LoanTransactionEntity, Long> {

    List<LoanTransactionEntity> getLoanTransactions(LoanRequestEntity loanRequestEntity);

    List<LoanTransactionEntity> getLoansPendingDisbursement(LoanTransactionTypeConstant loanTransactionType);

    List<LoanTransactionEntity> getLoansPendingRepayment(LoanTransactionTypeConstant loanTransactionType);
}
