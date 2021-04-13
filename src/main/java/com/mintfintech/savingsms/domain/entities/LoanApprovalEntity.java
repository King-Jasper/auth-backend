package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.LoanTransactionTypeConstant;
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
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "loan_approval")
public class LoanApprovalEntity extends AbstractBaseEntity<Long> {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    private LoanRequestEntity loanRequest;

    private String loanSuspenseCreditReference;

    private String loanSuspenseCreditResponseCode;

    private String loanIncomeSuspenseCreditReference;

    private String loanIncomeSuspenseCreditCode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LoanTransactionTypeConstant loanTransactionType;

    @OneToOne(fetch = FetchType.LAZY)
    private LoanTransactionEntity disbursementTransaction;
}
