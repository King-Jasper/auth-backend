package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.CorporateTransactionEntity;
import com.mintfintech.savingsms.domain.entities.CorporateTransactionRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorporateTransactionRepository extends JpaRepository<CorporateTransactionEntity, Long> {
    CorporateTransactionEntity getByTransactionRequest(CorporateTransactionRequestEntity requestEntity);
}
