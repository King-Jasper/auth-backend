package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.RoundUpSavingsTypeConstant;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Created by jnwanya on
 * Fri, 23 Oct, 2020
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roundup_savings_setting")
public class RoundUpSavingsSettingEntity extends AbstractBaseEntity<Long> {

    @OneToOne
    private MintAccountEntity account;

    @OneToOne
    private AppUserEntity creator;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoundUpSavingsTypeConstant fundTransferRoundUpType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoundUpSavingsTypeConstant cardPaymentRoundUpType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoundUpSavingsTypeConstant billPaymentRoundUpType;

    @Builder.Default
    private boolean enabled = false;

}
