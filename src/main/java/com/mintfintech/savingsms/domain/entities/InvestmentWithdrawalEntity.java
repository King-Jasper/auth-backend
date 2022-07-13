package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.InvestmentWithdrawalStageConstant;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentWithdrawalTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.WithholdingTaxStatusConstant;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Created by jnwanya on
 * Thu, 20 May, 2021
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "investment_withdrawal")
public class InvestmentWithdrawalEntity extends AbstractBaseEntity<Long>{

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private InvestmentEntity investment;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    private AppUserEntity requestedBy;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InvestmentWithdrawalStageConstant withdrawalStage;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InvestmentWithdrawalTypeConstant withdrawalType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Builder.Default
    private BigDecimal interest = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Builder.Default
    private BigDecimal amountCharged = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal balanceBeforeWithdrawal;

    @Column(nullable = false)
    private BigDecimal interestBeforeWithdrawal;

    @Builder.Default
    private boolean matured = true;

    private LocalDate dateForWithdrawal;

    @OneToOne(optional = true)
    private InvestmentTransactionEntity penaltyCharge;

    @OneToOne(optional = true)
    private InvestmentTransactionEntity withholdingTaxCharge;

    @OneToOne(optional = true)
    private InvestmentTransactionEntity principalPayout;

    @OneToOne(optional = true)
    private InvestmentTransactionEntity interestPayout;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    private MintBankAccountEntity creditAccount;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "String default NOT_APPLICABLE", nullable = false)
    private WithholdingTaxStatusConstant withholdingTaxStatus;


}
