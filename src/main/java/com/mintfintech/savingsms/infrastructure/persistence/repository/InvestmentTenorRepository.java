package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.InvestmentTenorEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestmentTenorRepository extends JpaRepository<InvestmentTenorEntity, Long> {

    Optional<InvestmentTenorEntity> findFirstByMinimumDurationAndMaximumDurationAndRecordStatus(int minDuration, int maxDuration, RecordStatusConstant statusConstant);
    List<InvestmentTenorEntity> getAllByRecordStatus(RecordStatusConstant statusConstant);

}
