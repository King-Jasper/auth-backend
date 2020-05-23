package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Tue, 04 Feb, 2020
 */
public interface MintAccountRepository extends JpaRepository<MintAccountEntity, Long> {
    Optional<MintAccountEntity> findFirstByAccountId(String accountId);
    List<MintAccountEntity> getAllByRecordStatus(RecordStatusConstant statusConstant);

    @Query("select m from MintAccountEntity m where id not in (select s.mintAccount.id from " +
            "SavingsGoalEntity s where s.savingsGoalType = com.mintfintech.savingsms.domain.entities.enums.SavingsGoalTypeConstant.MINT_DEFAULT_SAVINGS)")
    List<MintAccountEntity> mintAccountsWithoutSavingGoals();

    @Query("select count(m) from MintAccountEntity m where id not in (select s.mintAccount.id" +
            " from SavingsGoalEntity s where s.savingsGoalType = com.mintfintech.savingsms.domain" +
            ".entities.enums.SavingsGoalTypeConstant.MINT_DEFAULT_SAVINGS)")
    long countMintAccountsWithoutSavingGoals();
}

