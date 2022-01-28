package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.usecase.data.value_objects.RoundUpTransactionCategoryType;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "spend_and_save_transaction")
public class SpendAndSaveTransactionEntity extends AbstractBaseEntity<Long> {

    @ManyToOne(optional = false)
    private MintBankAccountEntity transactionAccount;

    @Column(nullable = false)
    private String transactionReference;

    @Column(nullable = false)
    private BigDecimal amountSaved;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private SavingsGoalEntity savingsGoal;

    @OneToOne(fetch = FetchType.LAZY)
    private SavingsGoalTransactionEntity savingsGoalTransaction;

    @Column(nullable = false)
    private BigDecimal transactionAmount;

    private LocalDateTime transactionDate;

    @ManyToOne
    private SpendAndSaveEntity spendAndSaveSetting;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoundUpTransactionCategoryType transactionType;


}
