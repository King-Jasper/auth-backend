package com.mintfintech.savingsms.domain.entities;

import lombok.Getter;
import lombok.Setter;
import javax.persistence.MappedSuperclass;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 17 May, 2021
 */
@Getter
@Setter
@MappedSuperclass
public abstract class InterestAccrual extends AbstractBaseEntity<Long>{

    private BigDecimal savingsBalance;

    private double rate;

    private BigDecimal interest;
}
