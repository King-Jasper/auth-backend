package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.CurrencyEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 04 Feb, 2020
 */
public interface CurrencyRepository extends JpaRepository<CurrencyEntity, Long> {
    Optional<CurrencyEntity> findFirstByCodeIgnoreCase(String code);
    List<CurrencyEntity> getAllByRecordStatus(RecordStatusConstant statusConstant);
}
