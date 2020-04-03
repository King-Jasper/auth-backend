package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.AppUserRepository;

import javax.inject.Named;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
@Named
public class AppUserEntityDaoImpl implements AppUserEntityDao {

    private AppUserRepository repository;
    public AppUserEntityDaoImpl(AppUserRepository repository) {
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
    public Optional<AppUserEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public AppUserEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. AppUser with id: "+aLong));
    }

    @Override
    public AppUserEntity saveRecord(AppUserEntity record) {
        return repository.save(record);
    }
}
