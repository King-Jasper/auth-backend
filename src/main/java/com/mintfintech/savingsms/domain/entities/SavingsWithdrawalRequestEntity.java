package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.WithdrawalRequestStatusConstant;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Created by jnwanya on
 * Tue, 07 Apr, 2020
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "savings_withdrawal_request")
public class SavingsWithdrawalRequestEntity extends AbstractBaseEntity<Long> {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUserEntity requestedBy;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private SavingsGoalEntity savingsGoal;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WithdrawalRequestStatusConstant withdrawalRequestStatus;

    @Builder.Default
    private boolean maturedGoal = false;

    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal savingsBalanceWithdrawal = BigDecimal.ZERO;

   // @ManyToOne(optional = false, fetch = FetchType.LAZY)
   // private MintBankAccountEntity creditAccount;



    @Builder.Default
    private BigDecimal interestWithdrawal = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal balanceBeforeWithdrawal = BigDecimal.ZERO;

    @Builder.Default
    private boolean interestCreditedOnDebitAccount = false;

    private String interestCreditResponseCode;

    private String interestCreditReference;

    @OneToOne(optional = true, fetch = FetchType.LAZY)
    private SavingsGoalTransactionEntity fundDisbursementTransaction;

    private LocalDate dateForWithdrawal;
}
