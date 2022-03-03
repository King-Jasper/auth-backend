package com.mintfintech.savingsms.domain.entities;

import lombok.*;
import org.apache.commons.lang3.StringUtils;

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

    @Builder.Default
    private boolean emailNotificationEnabled = true;

    @Builder.Default
    private boolean smsNotificationEnabled = true;;

    @Builder.Default
    private boolean gcmNotificationEnabled = true;;

    private String deviceGcmNotificationToken;

    private String residentialAddress;


    public String getFirstName() {
        return StringUtils.capitalize(name.split(" ", 2)[0].toLowerCase());
    }

}
