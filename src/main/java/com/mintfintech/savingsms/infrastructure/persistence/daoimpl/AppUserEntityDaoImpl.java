package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.AppUserRepository;

import javax.inject.Named;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
@Named
public class AppUserEntityDaoImpl extends CrudDaoImpl<AppUserEntity, Long> implements AppUserEntityDao {

    private final AppUserRepository repository;
    public AppUserEntityDaoImpl(AppUserRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public Optional<AppUserEntity> findAppUserByUserId(String userId) {
        return repository.findFirstByUserId(userId);
    }

    @Override
    public AppUserEntity getAppUserByUserId(String userId) {
        return findAppUserByUserId(userId).orElseThrow(() -> new RuntimeException("Not found. AppUser with userId: "+userId));
    }

    @Override
    public Optional<AppUserEntity> findAccountOwner(MintAccountEntity mintAccountEntity) {
        return repository.findFirstByPrimaryAccount(mintAccountEntity);
    }

    @Override
    public Optional<AppUserEntity> findAppUserByName(String customer) {
        return Optional.empty();
    }

    @Override
    public Optional<AppUserEntity> findUserByPhoneNumber(String phoneNumber) {
        return repository.findTopByPhoneNumberAndRecordStatus(phoneNumber, RecordStatusConstant.ACTIVE);
    }
}
