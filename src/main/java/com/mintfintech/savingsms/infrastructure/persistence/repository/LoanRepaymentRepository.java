package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.LoanRepaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoanRepaymentRepository extends JpaRepository<LoanRepaymentEntity, Long> {

    @Query("select s from LoanRepaymentEntity s where" +
            " s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " s.loanRecoveryCreditCode != ?1 and" +
            " s.transactionType = com.mintfintech.savingsms.domain.entities.enums.LoanTransactionTypeConstant.RECOVERY_TO_MINT" +
            " ORDER BY s.dateCreated DESC")
    List<LoanRepaymentEntity> getPendingRecoveryToMintTransaction(String responseCode);

    @Query("select s from LoanRepaymentEntity s where" +
            " s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " s.loanIncomeCreditCode != ?1 and" +
            " s.transactionType = com.mintfintech.savingsms.domain.entities.enums.LoanTransactionTypeConstant.SUSPENSE_TO_INCOME" +
            " ORDER BY s.dateCreated DESC")
    List<LoanRepaymentEntity> getPendingSuspenseToIncomeTransaction(String responseCode);
}
