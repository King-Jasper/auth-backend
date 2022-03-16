package com.mintfintech.savingsms.domain.dao;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;

import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.domain.models.InvestmentTransactionSearchDTO;

public interface InvestmentTransactionEntityDao extends CrudDao<InvestmentTransactionEntity, Long> {
	String generateTransactionReference();

	List<InvestmentTransactionEntity> getTransactionsByInvestment(InvestmentEntity investmentEntity,
			TransactionTypeConstant type, TransactionStatusConstant status);

	List<InvestmentTransactionEntity> getTransactionsByInvestment(InvestmentEntity investmentEntity);

	Page<InvestmentTransactionEntity> searchInvestmentTransactions(
			InvestmentTransactionSearchDTO investmentTransactionSearchDTO, int pageIndex, int size);

	BigDecimal sumSearchedInvestmentTransactions(InvestmentTransactionSearchDTO investmentTransactionSearchDTO);
}
