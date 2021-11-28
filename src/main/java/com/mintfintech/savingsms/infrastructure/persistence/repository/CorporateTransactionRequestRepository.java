package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.CorporateTransactionRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CorporateTransactionRequestRepository extends JpaRepository<CorporateTransactionRequestEntity, Long>, JpaSpecificationExecutor<CorporateTransactionRequestEntity> {
    Optional<CorporateTransactionRequestEntity> findTopByRequestIdAndRecordStatus(String requestId, RecordStatusConstant statusConstant);
}
