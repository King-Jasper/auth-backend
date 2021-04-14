package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.LoanRequestEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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

    List<LoanRequestEntity> getAllByRequestedByAndRecordStatus(AppUserEntity requestedBy, RecordStatusConstant recordStatus);

    List<LoanRequestEntity> getAllByRequestedByAndRecordStatusAndLoanType(AppUserEntity requestedBy, RecordStatusConstant recordStatus, LoanTypeConstant loanTypeConstant);

    @Query("select count(s) from LoanRequestEntity s where s.requestedBy = ?1" +
            " and s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " (s.repaymentStatus = com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant.PAID or" +
            " s.repaymentStatus = com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant.FAILED)")
    long countTotalLoans(AppUserEntity requestedBy);

    @Query("select count(s) from LoanRequestEntity s where s.requestedBy = ?1" +
            " and s.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and" +
            " s.repaymentStatus = com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant.FAILED")
    long countTotalLoansPastRepaymentDueDate(AppUserEntity requestedBy);

    @Query(value = "select r from LoanRequestEntity r where r.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and " +
            "(r.repaymentStatus = com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant.PENDING or " +
            "r.repaymentStatus = com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant.PARTIALLY_PAID) and " +
            "r.repaymentDueDate between :start and :end")
    List<LoanRequestEntity> getRepaymentPlansDueAtTime(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = "select r from LoanRequestEntity r where r.recordStatus = com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant.ACTIVE and " +
            "r.repaymentStatus = com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant.FAILED and " +
            "r.bankAccount = ?1 and " +
            "r.amountPaid < r.repaymentAmount")
    List<LoanRequestEntity> getOverdueLoanRepayment(MintBankAccountEntity mintBankAccountEntity);

}
