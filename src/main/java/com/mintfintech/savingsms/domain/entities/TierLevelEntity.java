package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.TierLevelTypeConstant;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;

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
@Table(name = "tier_level")
public class TierLevelEntity extends AbstractBaseEntity<Long> {

    private String tierId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TierLevelTypeConstant level;

    private BigDecimal bulletTransactionAmount;

    private BigDecimal maximumBalance;
}
