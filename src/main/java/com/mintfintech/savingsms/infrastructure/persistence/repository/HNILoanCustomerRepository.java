package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.HNILoanCustomerEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 26 Sep, 2023
 */
public interface HNILoanCustomerRepository extends JpaRepository<HNILoanCustomerEntity, Long>, JpaSpecificationExecutor<HNILoanCustomerEntity> {
    Optional<HNILoanCustomerEntity> findTopByCustomer(MintAccountEntity mintAccount);
}
