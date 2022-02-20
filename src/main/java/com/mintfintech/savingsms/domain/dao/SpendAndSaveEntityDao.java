package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SpendAndSaveEntity;

import java.util.Optional;

public interface SpendAndSaveEntityDao  extends CrudDao<SpendAndSaveEntity, Long> {
    Optional<SpendAndSaveEntity> findSpendAndSaveByAppUserAndMintAccount(AppUserEntity appUser, MintAccountEntity mintAccount);

    Optional<SpendAndSaveEntity> findSpendAndSaveSettingByAccount(MintAccountEntity mintAccount);
}
