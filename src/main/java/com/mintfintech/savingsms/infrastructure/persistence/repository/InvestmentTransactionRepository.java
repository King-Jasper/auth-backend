package com.mintfintech.savingsms.infrastructure.persistence.repository;

import java.util.List;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentStatusConstant;
import com.mintfintech.savingsms.domain.models.reports.ReportStatisticModel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import org.springframework.data.jpa.repository.Query;

public interface InvestmentTransactionRepository extends JpaRepository<InvestmentTransactionEntity, Long>, JpaSpecificationExecutor<InvestmentTransactionEntity> {

	List<InvestmentTransactionEntity> getAllByInvestmentAndTransactionTypeAndTransactionStatusOrderByDateCreatedDesc(InvestmentEntity investment, TransactionTypeConstant transactionType, TransactionStatusConstant transactionStatus);

	List<InvestmentTransactionEntity> getAllByRecordStatusAndInvestmentOrderByDateCreatedDesc(RecordStatusConstant statusConstant, InvestmentEntity investmentEntity);

	@Query("select new com.mintfintech.savingsms.domain.models.reports.ReportStatisticModel(count(distinct i.investment), sum(i.transactionAmount)) from InvestmentTransactionEntity i " +
			"where i.bankAccount.mintAccount = ?1 and i.transactionType = com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant.CREDIT and " +
			"i.transactionStatus = com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant.SUCCESSFUL")
	ReportStatisticModel getInvestmentTransactionStatistics(MintAccountEntity mintAccount);

    List<InvestmentTransactionEntity> getAllByRecordStatusAndBankAccountAndInvestment_InvestmentStatusOrderByDateCreatedDesc(RecordStatusConstant status, MintBankAccountEntity mintBankAccountEntity, InvestmentStatusConstant active, Pageable pageable);
}
