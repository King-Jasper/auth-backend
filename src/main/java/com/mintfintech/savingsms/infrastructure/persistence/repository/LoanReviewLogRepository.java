package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.LoanReviewLogEntity;
import com.mintfintech.savingsms.domain.entities.enums.LoanReviewLogType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by jnwanya on
 * Mon, 09 Aug, 2021
 */
public interface LoanReviewLogRepository extends JpaRepository<LoanReviewLogEntity, Long> {
    List<LoanReviewLogEntity> getAllByReviewLogTypeOrderByDateCreatedDesc(LoanReviewLogType reviewLogType);
    boolean existsAllByEntityIdAndReviewerId(long entityId, String reviewerId);
}
