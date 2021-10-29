package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.CorporateTransactionRequestEntity;
import com.mintfintech.savingsms.domain.models.reports.CorporateTransactionSearchDTO;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface CorporateTransactionRequestEntityDao extends CrudDao<CorporateTransactionRequestEntity, Long> {

    String generateRequestId();
    Optional<CorporateTransactionRequestEntity> findByRequestId(String requestId);
    Page<CorporateTransactionRequestEntity> searchTransaction(CorporateTransactionSearchDTO searchDTO, int page, int size);
}
