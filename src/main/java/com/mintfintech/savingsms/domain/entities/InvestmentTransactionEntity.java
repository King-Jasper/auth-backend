package com.mintfintech.savingsms.domain.entities;

import lombok.*;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
/**
 * Created by jnwanya on
 * Mon, 17 May, 2021
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "investment_transaction")
public class InvestmentTransactionEntity extends SavingsTransaction{

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private InvestmentEntity investment;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MintBankAccountEntity bankAccount;
}
