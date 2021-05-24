package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.TransactionCategory;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Created by jnwanya on
 * Fri, 29 May, 2020
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "accumulated_interest")
public class AccumulatedInterestEntity extends AbstractBaseEntity<Long>{

    @Column(nullable = false)
    private BigDecimal totalInterest;

    @Column(nullable = false)
    private LocalDate interestDate;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatusConstant transactionStatus;

    private String responseCode;

    private String responseMessage;

    private String externalReference;

    @Enumerated(EnumType.STRING)
    private TransactionCategory transactionCategory;
}
