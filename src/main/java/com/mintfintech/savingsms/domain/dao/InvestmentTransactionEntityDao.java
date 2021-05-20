package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;

import java.util.List;

public interface InvestmentTransactionEntityDao extends CrudDao<InvestmentTransactionEntity, Long>{
    String generateTransactionReference();

    List<InvestmentTransactionEntity> getTransactionsByInvestment(InvestmentEntity investmentEntity, TransactionTypeConstant type, TransactionStatusConstant status);
}
