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
}

