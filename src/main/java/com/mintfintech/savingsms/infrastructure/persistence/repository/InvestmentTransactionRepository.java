package com.mintfintech.savingsms.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;

public interface InvestmentTransactionRepository extends JpaRepository<InvestmentTransactionEntity, Long>,
		JpaSpecificationExecutor<InvestmentTransactionEntity> {

	List<InvestmentTransactionEntity> getAllByInvestmentAndTransactionTypeAndTransactionStatusOrderByDateCreatedDesc(
			InvestmentEntity investment, TransactionTypeConstant transactionType,
			TransactionStatusConstant transactionStatus);

	List<InvestmentTransactionEntity> getAllByRecordStatusAndInvestmentOrderByDateCreatedDesc(
			RecordStatusConstant statusConstant, InvestmentEntity investmentEntity);
}
