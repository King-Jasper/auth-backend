package com.mintfintech.savingsms.domain.entities;

import lombok.*;

import javax.persistence.*;

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
@Table(name = "app_user")
public class AppUserEntity extends AbstractBaseEntity<Long> {

    @Column(nullable = false, unique = true)
    private String userId;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    private MintAccountEntity primaryAccount;

    private String phoneNumber;

    private String name;

    private String email;

    private boolean emailNotificationEnabled;

    private boolean smsNotificationEnabled;

    private boolean gcmNotificationEnabled;

}
