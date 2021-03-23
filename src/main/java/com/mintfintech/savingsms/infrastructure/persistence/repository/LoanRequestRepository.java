package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LoanRequestRepository extends JpaRepository<LoanRequestEntity, Long>, JpaSpecificationExecutor<LoanRequestEntity> {

    Optional<LoanRequestEntity> findByLoanId(String loanId);

    @Query("select count(s) from LoanRequestEntity s where s.requestedBy = ?1" +
            " and s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " (s.repaymentStatus = com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant.PARTIALLY_PAID or" +
            " s.repaymentStatus = com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant.PENDING or" +
            " s.repaymentStatus = com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant.FAILED)")
    long countActiveCustomerLoanAccount(AppUserEntity requestedBy);

    @Query("select count(s) from LoanRequestEntity s where s.requestedBy = ?1" +
            " and s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " s.amountPaid < s.repaymentAmount")
    long countActiveCustomerLoanOnAccount(AppUserEntity requestedBy);
}
