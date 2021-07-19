package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;


import com.mintfintech.savingsms.domain.dao.CorporateUserEntityDao;
import com.mintfintech.savingsms.domain.entities.CorporateUserEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.CorporateUserRepository;

import javax.inject.Named;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Sun, 18 Jul, 2021
 */
@Named
public class CorporateUserEntityDaoImpl extends CrudDaoImpl<CorporateUserEntity, Long> implements CorporateUserEntityDao {

    private final CorporateUserRepository repository;
    public CorporateUserEntityDaoImpl(CorporateUserRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public Optional<CorporateUserEntity> findRecordByAccountIdAndUserId(String accountId, String userId) {
        return repository.findTopByAppUser_UserIdAndCorporateAccount_AccountId(accountId, userId);
    }
}
