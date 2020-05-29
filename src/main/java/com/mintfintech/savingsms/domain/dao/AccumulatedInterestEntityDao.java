package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.AccumulatedInterestEntity;

/**
 * Created by jnwanya on
 * Fri, 29 May, 2020
 */
public interface AccumulatedInterestEntityDao extends CrudDao<AccumulatedInterestEntity, Long>{
    String generatedReference();
}
