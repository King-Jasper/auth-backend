package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
public interface AppUserEntityDao extends CrudDao<AppUserEntity, Long> {
    Optional<AppUserEntity> findAppUserByUserId(String userId);
    AppUserEntity getAppUserByUserId(String userId);
}
