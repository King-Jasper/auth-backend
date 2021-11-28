package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.CorporateTransactionCategoryConstant;
import com.mintfintech.savingsms.domain.entities.enums.CorporateTransactionTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionApprovalStatusConstant;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "corporate_transaction_request")
public class CorporateTransactionRequestEntity extends AbstractBaseEntity<Long>{

    @Column(nullable = false, unique = true, updatable = false)
    private String requestId;

    @Column(nullable = false)
    private String debitAccountId;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private MintAccountEntity corporate;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private AppUserEntity initiator;

    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private CorporateTransactionCategoryConstant transactionCategory;

    @Enumerated(EnumType.STRING)
    private CorporateTransactionTypeConstant transactionType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionApprovalStatusConstant approvalStatus = TransactionApprovalStatusConstant.PENDING;

    @ManyToOne(fetch = FetchType.EAGER)
    private AppUserEntity reviewer;

    private LocalDateTime dateReviewed;

    private String statusUpdateReason;

    private String transactionDescription;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String transactionMetaData;


}
