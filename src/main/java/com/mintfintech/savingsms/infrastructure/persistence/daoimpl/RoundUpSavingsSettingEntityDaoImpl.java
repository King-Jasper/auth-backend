package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.RoundUpSavingsSettingEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.RoundUpSavingsSettingEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.RoundUpSavingsSettingRepository;
import javax.inject.Named;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
@Named
public class RoundUpSavingsSettingEntityDaoImpl extends CrudDaoImpl<RoundUpSavingsSettingEntity, Long> implements RoundUpSavingsSettingEntityDao {

    private final RoundUpSavingsSettingRepository repository;
    public RoundUpSavingsSettingEntityDaoImpl(RoundUpSavingsSettingRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public Optional<RoundUpSavingsSettingEntity> findRoundUpSavingsByUser(AppUserEntity user) {
        return repository.findTopByCreator(user);
    }

    @Override
    public Optional<RoundUpSavingsSettingEntity> findRoundUpSavingsByAccount(MintAccountEntity mintAccount) {
        return repository.findTopByAccount(mintAccount);
    }

    @Override
    public Optional<RoundUpSavingsSettingEntity> findActiveRoundUpSavingsByUser(AppUserEntity user) {
        return repository.findTopByCreatorAndEnabledTrue(user);
    }
}
