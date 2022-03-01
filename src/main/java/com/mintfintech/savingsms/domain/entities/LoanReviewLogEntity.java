package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.LoanReviewLogType;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * Created by jnwanya on
 * Mon, 09 Aug, 2021
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "loan_review_log")
public class LoanReviewLogEntity extends AbstractBaseEntity<Long> {

    private String reviewerName;

    private String reviewerId;

    private long entityId;

    private String description;

    @Enumerated(EnumType.STRING)
    private LoanReviewLogType reviewLogType;
}
