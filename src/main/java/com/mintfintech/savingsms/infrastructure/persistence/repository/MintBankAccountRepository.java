package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Sun, 09 Feb, 2020
 */
public interface MintBankAccountRepository extends JpaRepository<MintBankAccountEntity, Long> {
    Optional<MintBankAccountEntity> findFirstByAccountId(String accountId);
    Optional<MintBankAccountEntity> findFirstByAccountIdAndMintAccount(String accountId, MintAccountEntity accountEntity);
    Optional<MintBankAccountEntity> findFirstByAccountIdAndMintAccount_AccountId(String accountId, String mintAccountId);
    List<MintBankAccountEntity> getAllByMintAccount(MintAccountEntity mintAccountEntity);
    Optional<MintBankAccountEntity> findFirstByMintAccountAndAccountType(MintAccountEntity mintAccountEntity, BankAccountTypeConstant accountTypeConstant);
    Optional<MintBankAccountEntity> findFirstByAccountNumber(String accountNumber);

    // implemented this
    Optional<MintBankAccountEntity> findFirstByAccountName(String accountName);

}
