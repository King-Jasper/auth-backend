package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.BankAccountGroupConstant;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mint_bank_account")
public class MintBankAccountEntity extends AbstractBaseEntity<Long> {

    @Column(nullable = false, unique = true)
    private String accountId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MintAccountEntity mintAccount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BankAccountGroupConstant accountGroup;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BankAccountTypeConstant accountType;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String accountName;

    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    private BigDecimal dailyTransactionLimit;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private TierLevelEntity accountTierLevel;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private CurrencyEntity currency;

    private LocalDateTime balanceUpdateTime;
}
