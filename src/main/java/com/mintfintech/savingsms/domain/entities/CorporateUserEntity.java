package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.CorporateRoleTypeConstant;
import lombok.*;

import javax.persistence.*;

/**
 * Created by jnwanya on
 * Sun, 18 Jul, 2021
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "corporate_user")
public class CorporateUserEntity extends AbstractBaseEntity<Long>{

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MintAccountEntity corporateAccount;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private AppUserEntity appUser;

    private String userRole;

    private boolean director;
}
