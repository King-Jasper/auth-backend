package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
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

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "loan_transaction")
public class LoanTransactionEntity extends AbstractBaseEntity<Long>{

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private LoanRequestEntity loanRequest;

    @Column(nullable = false)
    private BigDecimal transactionAmount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatusConstant status;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionTypeConstant transactionType;

    @Builder.Default
    private boolean lienActive = false;

    @Column(nullable = false, unique = true)
    private String transactionReference;

    private String externalReference;

    private String lienReference;

    private String responseCode;

    private String responseMessage;



}
