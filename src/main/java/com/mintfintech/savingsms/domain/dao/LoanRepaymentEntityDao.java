package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.LoanRepaymentEntity;

import java.util.List;

public interface LoanRepaymentEntityDao extends CrudDao<LoanRepaymentEntity, Long>{

    List<LoanRepaymentEntity> getPendingRecoveryToMintTransaction(String responseCode);

    List<LoanRepaymentEntity> getPendingSuspenseToIncomeTransaction(String responseCode);
}
