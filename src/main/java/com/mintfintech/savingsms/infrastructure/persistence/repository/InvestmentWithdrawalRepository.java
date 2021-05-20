package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.InvestmentWithdrawalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by jnwanya on
 * Thu, 20 May, 2021
 */
public interface InvestmentWithdrawalRepository extends JpaRepository<InvestmentWithdrawalEntity, Long> {
}
