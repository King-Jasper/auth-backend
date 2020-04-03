package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.MintBankAccountRepository;

import javax.inject.Named;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
@Named
public class MintBankAccountEntityDaoImpl implements MintBankAccountEntityDao {

    private MintBankAccountRepository repository;
    public MintBankAccountEntityDaoImpl(MintBankAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MintBankAccountEntity> findByAccountId(String accountId) {
        return repository.findFirstByAccountId(accountId);
    }

    @Override
    public Optional<MintBankAccountEntity> findByAccountNumber(String accountNumber) {
        return repository.findFirstByAccountNumber(accountNumber);
    }

    @Override
    public Optional<MintBankAccountEntity> findByAccountIdAndMintAccount(String accountId, MintAccountEntity mintAccountEntity) {
        return repository.findFirstByAccountIdAndMintAccount(accountId, mintAccountEntity);
    }

    @Override
    public Optional<MintBankAccountEntity> findByAccountIdAndMintAccountId(String accountId, String mintAccountId) {
        return repository.findFirstByAccountIdAndMintAccount_AccountId(accountId, mintAccountId);
    }

    @Override
    public MintBankAccountEntity getAccountByMintAccountAndAccountType(MintAccountEntity mintAccountEntity, BankAccountTypeConstant accountTypeConstant) {
        return repository.findFirstByMintAccountAndAccountType(mintAccountEntity, accountTypeConstant)
                .orElseThrow(() -> new RuntimeException("Not found: MintBankAccountEntity for account: "+mintAccountEntity.getAccountId()));
    }

    @Override
    public List<MintBankAccountEntity> getAccountsByMintAccount(MintAccountEntity mintAccountEntity) {
        return repository.getAllByMintAccount(mintAccountEntity);
    }

    @Override
    public Optional<MintBankAccountEntity> findById(Long aLong) {
        return repository.findById(aLong);
    }

    @Override
    public MintBankAccountEntity getRecordById(Long aLong) throws RuntimeException {
        return findById(aLong).orElseThrow(() -> new RuntimeException("Not found. MintBankAccountEntity with Id: "+aLong));
    }

    @Override
    public MintBankAccountEntity saveRecord(MintBankAccountEntity record) {
        return repository.save(record);
    }
}
