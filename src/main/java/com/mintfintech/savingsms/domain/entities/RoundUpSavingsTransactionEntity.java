package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.RoundUpSavingsTypeConstant;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roundup_savings_transaction")
public class RoundUpSavingsTransactionEntity extends AbstractBaseEntity<Long> {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private SavingsGoalEntity savingsGoal;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MintBankAccountEntity transactionAccount;

    @Column(nullable = false)
    private String transactionReference;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoundUpSavingsTypeConstant savingsRoundUpType;

    @Column(nullable = false)
    private BigDecimal transactionAmount;

    @Column(nullable = false)
    private BigDecimal amountSaved;

    @OneToOne(optional = true, fetch = FetchType.LAZY)
    private SavingsGoalTransactionEntity transaction;

}
