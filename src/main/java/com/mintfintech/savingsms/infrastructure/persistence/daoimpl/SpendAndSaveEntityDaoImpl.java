package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.CrudDao;
import com.mintfintech.savingsms.domain.dao.SpendAndSaveEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SpendAndSaveEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SpendAndSaveRepository;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Optional;

@Named
@AllArgsConstructor
public class SpendAndSaveEntityDaoImpl implements SpendAndSaveEntityDao {

    private final SpendAndSaveRepository repository;

    @Override
    public Optional<SpendAndSaveEntity> findById(Long aLong) {
        return Optional.empty();
    }

    @Override
    public SpendAndSaveEntity getRecordById(Long aLong) throws RuntimeException {
        return null;
    }

    @Override
    public SpendAndSaveEntity saveRecord(SpendAndSaveEntity record) {
        return null;
    }

    @Override
    public Optional<SpendAndSaveEntity> findSpendAndSaveByAppUserAndMintAccount(AppUserEntity appUser, MintAccountEntity mintAccount) {
        return repository.findTopByCreatorAndAccountAndRecordStatus(appUser, mintAccount, RecordStatusConstant.ACTIVE);
    }

    @Override
    public Optional<SpendAndSaveEntity> findSpendAndSaveSettingByAccount(MintAccountEntity mintAccount) {
        return repository.findTopByAccountAndRecordStatus(mintAccount, RecordStatusConstant.ACTIVE);
    }
}
