package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.InvestmentTransactionTypeConstant;
import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "investment_withdrawal_transaction")
public class InvestmentWithdrawalTransaction {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    private InvestmentWithdrawalEntity investmentWithdrawal;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InvestmentTransactionTypeConstant investmentTransactionType;
}
