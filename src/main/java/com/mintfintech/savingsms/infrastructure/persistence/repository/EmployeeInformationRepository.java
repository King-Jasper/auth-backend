package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.EmployeeInformationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeInformationRepository extends JpaRepository<EmployeeInformationEntity, Long> {
}
