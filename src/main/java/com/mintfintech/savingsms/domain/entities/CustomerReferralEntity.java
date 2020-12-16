package com.mintfintech.savingsms.domain.entities;

import lombok.*;

import javax.persistence.*;

/**
 * Created by jnwanya on
 * Tue, 15 Dec, 2020
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_referral")
public class CustomerReferralEntity extends AbstractBaseEntity<Long>{

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private MintAccountEntity referrer;

    @Column(nullable = false)
    private String referralCode;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    private MintAccountEntity referred;

    private boolean referrerRewarded = false;

    private boolean referredRewarded = false;
}
