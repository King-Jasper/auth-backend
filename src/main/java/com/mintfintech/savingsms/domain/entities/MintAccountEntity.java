package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.AccountTypeConstant;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Sat, 22 Feb, 2020
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mint_account")
public class MintAccountEntity extends AbstractBaseEntity<Long> {

    @Column(nullable = false, unique = true)
    private String accountId;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountTypeConstant accountType = AccountTypeConstant.INDIVIDUAL;

}
