package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.CurrencyEntity;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
public interface CurrencyEntityDao extends CrudDao<CurrencyEntity, Long> {
    Optional<CurrencyEntity> findByCode(String code);
    CurrencyEntity getByCode(String code);
    long countRecords();
}
