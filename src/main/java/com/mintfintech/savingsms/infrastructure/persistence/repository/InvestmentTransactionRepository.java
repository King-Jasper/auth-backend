package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.InvestmentTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentTransactionRepository extends JpaRepository<InvestmentTransactionEntity, Long> {
}
