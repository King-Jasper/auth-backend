package com.mintfintech.savingsms.domain.dao;


import com.mintfintech.savingsms.domain.entities.CorporateUserEntity;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Sun, 18 Jul, 2021
 */
public interface CorporateUserEntityDao extends CrudDao<CorporateUserEntity, Long> {
    Optional<CorporateUserEntity> findRecordByAccountIdAndUserId(String accountId, String userId);
}
