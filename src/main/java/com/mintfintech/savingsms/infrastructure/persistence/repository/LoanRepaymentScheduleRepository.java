package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.LoanRepaymentScheduleEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by jnwanya on
 * Wed, 27 Sep, 2023
 */
public interface LoanRepaymentScheduleRepository extends JpaRepository<LoanRepaymentScheduleEntity, Long> {
    List<LoanRepaymentScheduleEntity> getAllByRecordStatusAndLoanRequestOrderByRepaymentDueDateAsc(RecordStatusConstant statusConstant, LoanRequestEntity loanRequest);
}
