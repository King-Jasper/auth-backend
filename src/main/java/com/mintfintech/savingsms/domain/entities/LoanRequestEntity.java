package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.LoanTypeConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "loan_request")
public class LoanRequestEntity extends AbstractBaseEntity<Long> {

    @Column(nullable = false, unique = true)
    private String loanId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MintBankAccountEntity bankAccount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUserEntity requestedBy;

    @Column(nullable = false)
    private BigDecimal loanAmount;

    @Builder.Default
    private BigDecimal loanInterest = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal repaymentAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal amountCollectedOnBankOne = BigDecimal.ZERO;

    @Builder.Default
    private double interestRate = 0.0;

    private LocalDateTime approvedDate;

    private LocalDateTime repaymentDueDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApprovalStatusConstant approvalStatus = ApprovalStatusConstant.PENDING;

    private String approveByName;

    private String approveByUserId;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LoanRepaymentStatusConstant repaymentStatus = LoanRepaymentStatusConstant.PENDING;

    @Enumerated(EnumType.STRING)
    private LoanTypeConstant loanType;

    private String trackingReference;

    private String accountNumber;

}
