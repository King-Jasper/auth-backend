package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.SavingsPlanChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
public interface SavingsPlanChangeRepository extends JpaRepository<SavingsPlanChangeEntity, Long> {
}
