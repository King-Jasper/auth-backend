package com.mintfintech.savingsms.domain.entities;


import com.mintfintech.savingsms.domain.entities.enums.FundingSourceTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Tue, 31 Mar, 2020
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "savings_goal_transaction")
public class SavingsGoalTransactionEntity extends SavingsTransaction {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private SavingsGoalEntity savingsGoal;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MintBankAccountEntity bankAccount;

    /*
    @Column(nullable = false)
    private BigDecimal transactionAmount;

    @Builder.Default
    @Column(nullable = false)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal newBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionTypeConstant transactionType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatusConstant transactionStatus;

    @Enumerated(EnumType.STRING)
    private FundingSourceTypeConstant fundingSource;

    @Column(nullable = false, unique = true)
    private String transactionReference;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    private AppUserEntity performedBy;

    private String transactionResponseCode;

    private String transactionResponseMessage;

    private String externalReference;
    */

}
