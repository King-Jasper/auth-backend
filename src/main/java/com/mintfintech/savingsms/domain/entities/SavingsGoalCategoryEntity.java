package com.mintfintech.savingsms.domain.entities;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by jnwanya on
 * Wed, 01 Apr, 2020
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "savings_goal_category")
public class SavingsGoalCategoryEntity extends AbstractBaseEntity<Long> {

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;
}
