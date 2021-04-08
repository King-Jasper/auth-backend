package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.ApprovalStatusConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "employee_information")
public class EmployeeInformationEntity extends AbstractBaseEntity<Long>{

    @Column(nullable = false)
    private BigDecimal monthlyIncome;

    @Column(nullable = false)
    private String organizationName;

    @Column(nullable = false)
    private String organizationUrl;

    @Column(nullable = false)
    private String employerAddress;

    @Column(nullable = false)
    private String employerEmail;

    @Column(nullable = false)
    private String employerPhoneNo;

    @Column(nullable = false)
    private String workEmail;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    private ResourceFileEntity employmentLetter;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ApprovalStatusConstant verificationStatus = ApprovalStatusConstant.PENDING;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

}
