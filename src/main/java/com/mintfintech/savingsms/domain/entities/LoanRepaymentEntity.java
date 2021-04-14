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
@Table(name = "loan_repayment")
public class LoanRepaymentEntity extends AbstractBaseEntity<Long> {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    private LoanRequestEntity loanRequest;

    private String loanRecoveryCreditReference;

    private String loanRecoveryCreditCode;

    private String loanIncomeCreditCode;

    private String loanIncomeCreditReference;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LoanTransactionTypeConstant loanTransactionType;
}
