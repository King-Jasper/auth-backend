package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.CorporateTransactionEntity;
import com.mintfintech.savingsms.domain.entities.CorporateTransactionRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.CorporateTransactionTypeConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CorporateTransactionRepository extends JpaRepository<CorporateTransactionEntity, Long> {
    Optional<CorporateTransactionEntity> findByTransactionRequest(CorporateTransactionRequestEntity requestEntity);
    Optional<CorporateTransactionEntity> findByTransactionRequestAndTransactionType(CorporateTransactionRequestEntity requestEntity, CorporateTransactionTypeConstant transactionType);
}
