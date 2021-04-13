package com.mintfintech.savingsms.domain.dao;


import com.mintfintech.savingsms.domain.entities.enums.SequenceType;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
public interface AppSequenceEntityDao {
    Long getNextSequenceId(SequenceType sequenceType);
    Long getNextSequenceIdTemp(SequenceType sequenceType);
}
