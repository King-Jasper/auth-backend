package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestmentTransactionRepository extends JpaRepository<InvestmentTransactionEntity, Long> {

    List<InvestmentTransactionEntity> getAllByInvestmentAndTransactionTypeAndTransactionStatus(InvestmentEntity investment, TransactionTypeConstant transactionType, TransactionStatusConstant transactionStatus);
}