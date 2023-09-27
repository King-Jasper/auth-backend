package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.LoanRepaymentPlanTypeConstant;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Created by jnwanya on
 * Tue, 26 Sep, 2023
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "hni_loan_customer")
public class HNILoanCustomerEntity extends AbstractBaseEntity<Long> {

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    private MintAccountEntity customer;

    private double interestRate;

    private boolean chequeRequired;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanRepaymentPlanTypeConstant repaymentPlanType;

    private LocalDateTime dateIssueLoanDueDate;

    private String lastProfiledBy;
}
