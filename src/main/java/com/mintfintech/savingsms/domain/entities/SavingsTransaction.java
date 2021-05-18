package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.FundingSourceTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 17 May, 2021
 */
@Getter
@Setter
@MappedSuperclass
public abstract class SavingsTransaction extends AbstractBaseEntity<Long> {

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
}
