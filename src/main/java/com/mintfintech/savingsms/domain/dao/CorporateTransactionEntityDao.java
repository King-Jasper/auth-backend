package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.CorporateTransactionEntity;
import com.mintfintech.savingsms.domain.entities.CorporateTransactionRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.CorporateTransactionTypeConstant;

public interface CorporateTransactionEntityDao extends CrudDao<CorporateTransactionEntity, Long> {
    CorporateTransactionEntity getByTransactionRequest(CorporateTransactionRequestEntity requestEntity);
    CorporateTransactionEntity getByTransactionRequestAndTransactionType(CorporateTransactionRequestEntity requestEntity, CorporateTransactionTypeConstant mutualInvestment);
}
