package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.AccumulatedInterestEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by jnwanya on
 * Fri, 29 May, 2020
 */
public interface AccumulatedInterestEntityDao extends CrudDao<AccumulatedInterestEntity, Long>{
    String generatedReference();
    List<AccumulatedInterestEntity> getFailedInterestRecord(LocalDateTime fromDate, LocalDateTime toDate);
}
