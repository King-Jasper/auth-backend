package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;


import com.mintfintech.savingsms.domain.dao.CorporateUserEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CorporateUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.CorporateUserRepository;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;

import javax.inject.Named;
import java.util.List;
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

    @Override
    public CorporateUserEntity getRecordByAccountIdAndUserId(MintAccountEntity corporateAccount, AppUserEntity user) {
        return findRecordByAccountAndUser(corporateAccount, user)
                .orElseThrow(() -> new BusinessLogicConflictException("Sorry, user not found for corporate account"));
    }

    @Override
    public Optional<CorporateUserEntity> findRecordByAccountAndUser(MintAccountEntity corporateAccount, AppUserEntity user) {
        return repository.findTopByAppUserAndCorporateAccount(user, corporateAccount);
    }

    @Override
    public List<CorporateUserEntity> findRecordByAccount(MintAccountEntity mintAccount) {
        return repository.findAllByCorporateAccount(mintAccount);
    }

    @Override
    public Optional<CorporateUserEntity> findTopByAppUser(AppUserEntity appUser) {
        return repository.findTopByAppUserAndRecordStatus(appUser, RecordStatusConstant.ACTIVE);
    }
}
