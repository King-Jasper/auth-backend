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
@Table(name = "investment_maturity_transaction")
public class InvestmentMaturityTransaction {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    private InvestmentTransactionEntity investmentTransaction;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InvestmentTransactionTypeConstant investmentTransactionType;
}
