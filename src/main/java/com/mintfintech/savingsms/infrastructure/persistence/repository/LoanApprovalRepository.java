package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.LoanApprovalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoanApprovalRepository extends JpaRepository<LoanApprovalEntity, Long> {

    @Query("select s from LoanApprovalEntity s where" +
            " s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " s.transactionType = com.mintfintech.savingsms.domain.entities.enums.LoanTransactionTypeConstant.PENDING_MINT_TO_SUSPENSE" +
            " ORDER BY s.dateCreated DESC")
    List<LoanApprovalEntity> getPendingMintToSuspenseTransaction();

    @Query("select s from LoanApprovalEntity s where" +
            " s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " s.transactionType = com.mintfintech.savingsms.domain.entities.enums.LoanTransactionTypeConstant.PENDING_INTEREST_TO_SUSPENSE" +
            " ORDER BY s.dateCreated DESC")
    List<LoanApprovalEntity> getPendingInterestToSuspenseTransaction();

    @Query("select s from LoanApprovalEntity s where" +
            " s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " s.transactionType = com.mintfintech.savingsms.domain.entities.enums.LoanTransactionTypeConstant.PENDING_SUSPENSE_TO_CUSTOMER" +
            " ORDER BY s.dateCreated DESC")
    List<LoanApprovalEntity> getPendingSuspenseToCustomerTransaction();
}
