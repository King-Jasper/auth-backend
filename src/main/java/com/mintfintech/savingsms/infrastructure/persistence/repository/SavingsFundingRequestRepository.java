package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.SavingsFundingRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Thu, 22 Oct, 2020
 */
public interface SavingsFundingRequestRepository extends JpaRepository<SavingsFundingRequestEntity, Long> {
    Optional<SavingsFundingRequestEntity> findTopByPaymentReference(String paymentReference);
}
