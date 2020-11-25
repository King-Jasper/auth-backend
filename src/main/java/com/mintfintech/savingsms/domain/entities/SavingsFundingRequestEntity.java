package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.PaymentGatewayTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionStatusConstant;
import lombok.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Created by jnwanya on
 * Thu, 22 Oct, 2020
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "savings_funding_request")
public class SavingsFundingRequestEntity extends AbstractBaseEntity<Long>{

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private SavingsGoalEntity savingsGoal;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUserEntity creator;

    @Column(nullable = false, unique = true)
    private String paymentReference;

    @Column(nullable = false, unique = true)
    private String fundingReference;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatusConstant paymentStatus = TransactionStatusConstant.PENDING;

    @Builder.Default
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentGatewayTypeConstant paymentGateway = PaymentGatewayTypeConstant.PAYSTACK;

    @Column(nullable = false)
    private BigDecimal amount;

    private LocalDateTime paymentDate;

    @OneToOne(optional = true)
    private SavingsGoalTransactionEntity fundingTransaction;
}
