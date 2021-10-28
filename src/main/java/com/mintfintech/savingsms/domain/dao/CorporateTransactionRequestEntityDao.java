package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.CorporateTransactionRequestEntity;

import java.util.Optional;

public interface CorporateTransactionRequestEntityDao extends CrudDao<CorporateTransactionRequestEntity, Long> {

    String generateRequestId();
    Optional<CorporateTransactionRequestEntity> findByRequestId(String requestId);
}
