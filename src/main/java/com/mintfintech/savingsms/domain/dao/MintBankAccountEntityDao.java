package com.mintfintech.savingsms.domain.dao;


import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;

import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
public interface MintBankAccountEntityDao extends CrudDao<MintBankAccountEntity, Long> {
    Optional<MintBankAccountEntity> findByAccountId(String accountId);
    Optional<MintBankAccountEntity> findByAccountNumber(String accountNumber);
    Optional<MintBankAccountEntity> findByAccountIdAndMintAccount(String accountId, MintAccountEntity mintAccountEntity);
    Optional<MintBankAccountEntity> findByAccountIdAndMintAccountId(String accountId, String mintAccountId);
    MintBankAccountEntity getAccountByMintAccountAndAccountType(MintAccountEntity mintAccountEntity, BankAccountTypeConstant accountTypeConstant);
    List<MintBankAccountEntity> getAccountsByMintAccount(MintAccountEntity mintAccountEntity);

}
