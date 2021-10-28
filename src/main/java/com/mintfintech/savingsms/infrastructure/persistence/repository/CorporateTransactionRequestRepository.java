package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.CorporateTransactionRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CorporateTransactionRequestRepository extends JpaRepository<CorporateTransactionRequestEntity, Long>, JpaSpecificationExecutor<CorporateTransactionRequestEntity> {
    Optional<CorporateTransactionRequestEntity> findTopByRequestId(String requestId);
}
