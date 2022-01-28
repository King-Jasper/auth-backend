package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SpendAndSaveEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpendAndSaveRepository extends JpaRepository<SpendAndSaveEntity, Long> {
    Optional<SpendAndSaveEntity> findTopByCreatorAndAccountAndRecordStatus(AppUserEntity appUser, MintAccountEntity mintAccount, RecordStatusConstant active);
    Optional<SpendAndSaveEntity> findTopByAccountAndRecordStatus(MintAccountEntity mintAccount, RecordStatusConstant active);
}
