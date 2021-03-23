package com.mintfintech.savingsms.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "customer_loan_profile")
public class CustomerLoanProfileEntity extends AbstractBaseEntity<Long> {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    private AppUserEntity user;

    @Builder.Default
    private boolean blacklisted = false;

    @Column(columnDefinition = "TEXT")
    private String blacklistReason;

    @Builder.Default
    private double rating = 0.0;

    @OneToOne(fetch = FetchType.LAZY)
    private EmployeeInformationEntity employeeInformation;

}
