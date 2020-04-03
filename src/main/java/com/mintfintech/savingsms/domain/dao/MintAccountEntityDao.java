package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Sat, 22 Feb, 2020
 */
public interface MintAccountEntityDao extends CrudDao<MintAccountEntity, Long> {
    Optional<MintAccountEntity> findAccountByAccountId(String accountId);
    MintAccountEntity getAccountByAccountId(String accountId);
}
