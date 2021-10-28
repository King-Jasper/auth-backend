package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.CorporateTransactionTypeConstant;
import lombok.*;

import javax.persistence.*;
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "corporate_transaction")
public class CorporateTransactionEntity extends AbstractBaseEntity<Long> {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private MintAccountEntity corporate;

    @ManyToOne(fetch = FetchType.LAZY)
    private CorporateTransactionRequestEntity transactionRequest;

    private long transactionRecordId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CorporateTransactionTypeConstant transactionType;
}
