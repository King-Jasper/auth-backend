package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.CorporateTransactionEntity;
import com.mintfintech.savingsms.domain.entities.CorporateTransactionRequestEntity;

import java.util.List;

public interface CorporateTransactionEntityDao extends CrudDao<CorporateTransactionEntity, Long> {
    CorporateTransactionEntity getByTransactionRequest(CorporateTransactionRequestEntity requestEntity);
}
