package com.mintfintech.savingsms.domain.entities;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Created by jnwanya on
 * Thu, 03 Mar, 2022
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "reacthq_referral")
public class ReactHQReferralEntity extends AbstractBaseEntity<Long> {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private MintAccountEntity customer;

    private boolean customerCredited;

    private String creditResponseCode;

    private String creditResponseMessage;

    private boolean customerDebited;

    private String debitResponseCode;

    private String debitResponseMessage;

    private String registrationPlatform;
}
