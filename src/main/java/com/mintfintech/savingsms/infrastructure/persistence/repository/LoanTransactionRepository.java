package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.LoanTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoanTransactionRepository extends JpaRepository<LoanTransactionEntity, Long> {

    @Query("select s from LoanTransactionEntity s where" +
            " s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " s.loanRequest = ?1 ORDER BY s.dateCreated DESC ")
    List<LoanTransactionEntity> getAllByRecordStatusAndLoanRequest(LoanRequestEntity loanRequestEntity);

    @Query("select s from LoanTransactionEntity s where" +
            " s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " s.transactionType = com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant.DEBIT and" +
            " s.loanRequest = ?1 ORDER BY s.dateCreated DESC ")
    List<LoanTransactionEntity> getDebitTransactionByLoanRequest(LoanRequestEntity loanRequestEntity);
}
