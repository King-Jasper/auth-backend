package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.LoanReviewLogEntity;
import com.mintfintech.savingsms.domain.entities.enums.LoanReviewLogType;

import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Mon, 09 Aug, 2021
 */
public interface LoanReviewLogEntityDao extends CrudDao<LoanReviewLogEntity, Long> {
    List<LoanReviewLogEntity> getRecordByReviewType(LoanReviewLogType reviewLogType);
    boolean recordExistForUserIdAndEntityId(String reviewerId, long entityId);
}
