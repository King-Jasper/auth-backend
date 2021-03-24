package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CustomerLoanProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerLoanProfileRepository extends JpaRepository<CustomerLoanProfileEntity, Long> {

    Optional<CustomerLoanProfileEntity> findByUser(AppUserEntity appUserEntity);

    @Query("select * from CustomerLoanProfileEntity s where" +
            " s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " s.employeeInformation.verified = ?2 and" +
            " s.blacklisted = ?1")
    List<CustomerLoanProfileEntity> findCustomerEmployeeInformation(boolean blacklisted, boolean employeeInfoVerified);
}
