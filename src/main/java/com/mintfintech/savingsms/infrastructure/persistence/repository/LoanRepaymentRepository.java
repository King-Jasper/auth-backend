package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.LoanRepaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoanRepaymentRepository extends JpaRepository<LoanRepaymentEntity, Long> {

    @Query("select s from LoanRepaymentEntity s where" +
            " s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " s.loanTransactionType = com.mintfintech.savingsms.domain.entities.enums.LoanTransactionTypeConstant.PENDING_RECOVERY_TO_MINT" +
            " ORDER BY s.dateCreated DESC")
    List<LoanRepaymentEntity> getPendingRecoveryToMintTransaction();

    @Query("select s from LoanRepaymentEntity s where" +
            " s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " s.loanTransactionType = com.mintfintech.savingsms.domain.entities.enums.LoanTransactionTypeConstant.PENDING_SUSPENSE_TO_INCOME" +
            " ORDER BY s.dateCreated DESC")
    List<LoanRepaymentEntity> getPendingSuspenseToIncomeTransaction();
}
