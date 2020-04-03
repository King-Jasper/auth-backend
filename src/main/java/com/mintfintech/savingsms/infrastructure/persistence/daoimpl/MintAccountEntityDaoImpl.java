package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.MintAccountRepository;

import javax.inject.Named;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Sat, 22 Feb, 2020
 */
@Named
public class MintAccountEntityDaoImpl implements MintAccountEntityDao {

    private MintAccountRepository repository;
    public MintAccountEntityDaoImpl(MintAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MintAccountEntity> findAccountByAccountId(String accountId) {
        return repository.findFirstByAccountId(accountId);
    }

    @Override
    public MintAccountEntity getAccountByAccountId(String accountId) {
        return findAccountByAccountId(accountId).orElseThrow(() -> new RuntimeException("Not found. MintAccountEntity with accountId "+accountId));
    }

    @Override
    public Optional<MintAccountEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public MintAccountEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. MintAccountEntity with Id :"+aLong));
    }

    @Override
    public MintAccountEntity saveRecord(MintAccountEntity record) {
        return repository.save(record);
    }
}
