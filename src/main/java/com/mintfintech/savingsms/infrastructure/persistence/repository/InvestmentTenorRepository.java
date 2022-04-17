package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.InvestmentTenorEntity;
import com.mintfintech.savingsms.domain.entities.SavingsPlanTenorEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InvestmentTenorRepository extends JpaRepository<InvestmentTenorEntity, Long> {

    Optional<InvestmentTenorEntity> findFirstByMinimumDurationAndMaximumDurationAndRecordStatus(int minDuration, int maxDuration, RecordStatusConstant statusConstant);
    List<InvestmentTenorEntity> getAllByRecordStatus(RecordStatusConstant statusConstant);

    @Query("select i from InvestmentTenorEntity i where i.recordStatus = ?2 and i.maximumDuration >= ?1 and i.minimumDuration <= ?1 order by i.dateCreated asc")
    List<InvestmentTenorEntity> findInvestmentTenorForDuration(int duration, RecordStatusConstant status);
}
