package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.RoundUpSavingsSettingEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.RoundUpSavingsSettingEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.RoundUpSavingsSettingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.inject.Named;
import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
@Named
public class RoundUpSavingsSettingEntityDaoImpl extends CrudDaoImpl<RoundUpSavingsSettingEntity, Long> implements RoundUpSavingsSettingEntityDao {

    private final RoundUpSavingsSettingRepository repository;
    public RoundUpSavingsSettingEntityDaoImpl(RoundUpSavingsSettingRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public Optional<RoundUpSavingsSettingEntity> findRoundUpSavingsByUser(AppUserEntity user) {
        return repository.findTopByCreatorAndRecordStatus(user, RecordStatusConstant.ACTIVE);
    }

    @Override
    public Optional<RoundUpSavingsSettingEntity> findRoundUpSavingsByAccount(MintAccountEntity mintAccount) {
        return repository.findTopByAccountAndRecordStatus(mintAccount, RecordStatusConstant.ACTIVE);
    }

    @Override
    public Optional<RoundUpSavingsSettingEntity> findActiveRoundUpSavingsByUser(AppUserEntity user) {
        return repository.findTopByCreatorAndEnabledTrueAndRecordStatus(user, RecordStatusConstant.ACTIVE);
    }

    @Override
    public List<RoundUpSavingsSettingEntity> getDeactivateSavingsWithZeroBalance(LocalDateTime deactivatedBeforeTime, int size) {
        Pageable pageable = PageRequest.of(0, size);
        return repository.getDeactivatedSavingsForDeletion(BigDecimal.ZERO, deactivatedBeforeTime, pageable);
    }

    @Transactional
    @Override
    public void deleteRecord(RoundUpSavingsSettingEntity savingsSettingEntity) {
        repository.delete(savingsSettingEntity);
    }
}
