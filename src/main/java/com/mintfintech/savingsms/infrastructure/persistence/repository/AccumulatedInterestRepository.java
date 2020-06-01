package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.AccumulatedInterestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by jnwanya on
 * Fri, 29 May, 2020
 */
public interface AccumulatedInterestRepository extends JpaRepository<AccumulatedInterestEntity, Long> {
}
