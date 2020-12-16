package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.SettingsNameTypeConstant;
import lombok.*;

import javax.persistence.*;

/**
 * Created by jnwanya on
 * Wed, 16 Dec, 2020
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "settings")
public class SettingsEntity extends AbstractBaseEntity<Long>{

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SettingsNameTypeConstant name;

    @Column(nullable = false)
    private String value;
}
