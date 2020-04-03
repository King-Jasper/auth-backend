package com.mintfintech.savingsms.domain.entities;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by jnwanya on
 * Mon, 03 Feb, 2020
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "currency")
public class CurrencyEntity extends AbstractBaseEntity<Long> {

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String symbol;
}
