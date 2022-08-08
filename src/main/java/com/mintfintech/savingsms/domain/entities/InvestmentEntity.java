package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.InvestmentStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.InvestmentTypeConstant;
import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
@Table(name = "investment")
public class InvestmentEntity extends AbstractBaseEntity<Long>{

    @Column(nullable = false, unique = true, updatable = false)
    private String code;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private MintAccountEntity owner;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private AppUserEntity creator;

    @Builder.Default
    @Column(nullable = false)
    private BigDecimal amountInvested = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private InvestmentStatusConstant investmentStatus;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private InvestmentTypeConstant investmentType = InvestmentTypeConstant.MUTUAL_INVESTMENT;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private InvestmentTenorEntity investmentTenor;

    private double interestRate;

    private int durationInMonths;

    private double maxLiquidateRate;

    @Column(nullable = false)
    private LocalDateTime maturityDate;

    @Builder.Default
    private boolean lockedInvestment = true;

    private LocalDateTime lastInterestApplicationDate;

    private LocalDateTime dateWithdrawn;

    @Builder.Default
    private BigDecimal totalAmountWithdrawn = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalAmountInvested = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalInterestWithdrawn = BigDecimal.ZERO;

    private String managedByUser;

    private String managedByUserId;

    private String referralCode;

    @Column(columnDefinition = "boolean default false")
    private boolean isAffiliateReferred;

}
