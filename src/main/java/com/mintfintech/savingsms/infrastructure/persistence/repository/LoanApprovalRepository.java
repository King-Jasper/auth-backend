package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.LoanApprovalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoanApprovalRepository extends JpaRepository<LoanApprovalEntity, Long> {

    @Query("select s from LoanApprovalEntity s where" +
            " s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " s.loanSuspenseCreditResponseCode != ?1 and" +
            " s.transactionType = com.mintfintech.savingsms.domain.entities.enums.LoanTransactionTypeConstant.MINT_TO_SUSPENSE" +
            " ORDER BY s.dateCreated DESC")
    List<LoanApprovalEntity> getPendingMintToSuspenseTransaction(String responseCode);

    @Query("select s from LoanApprovalEntity s where" +
            " s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " s.loanIncomeSuspenseCreditCode != ?1 and" +
            " s.transactionType = com.mintfintech.savingsms.domain.entities.enums.LoanTransactionTypeConstant.INTEREST_TO_SUSPENSE" +
            " ORDER BY s.dateCreated DESC")
    List<LoanApprovalEntity> getPendingInterestToSuspenseTransaction(String responseCode);

    @Query("select s from LoanApprovalEntity s where" +
            " s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " s.disbursementTransaction.status = com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant.PENDING" +
            " s.transactionType = com.mintfintech.savingsms.domain.entities.enums.LoanTransactionTypeConstant.SUSPENSE_TO_CUSTOMER" +
            " ORDER BY s.dateCreated DESC")
    List<LoanApprovalEntity> getPendingSuspenseToCustomerTransaction();
}
