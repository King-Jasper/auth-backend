package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.LoanApprovalEntity;

import java.util.List;

public interface LoanApprovalEntityDao extends CrudDao<LoanApprovalEntity, Long> {

    List<LoanApprovalEntity> getPendingMintToSuspenseTransaction();

    List<LoanApprovalEntity> getPendingInterestToSuspenseTransaction();

    List<LoanApprovalEntity> getPendingSuspenseToCustomerTransaction();

    LoanApprovalEntity saveAndFlush(LoanApprovalEntity loanApprovalEntity);

}
