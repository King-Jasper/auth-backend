package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.RoundUpSavingsSettingEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.RoundUpSavingsSettingEntity;
import com.mintfintech.savingsms.infrastructure.persistence.repository.RoundUpSavingsSettingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import javax.inject.Named;
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
        return repository.findTopByCreator(user);
    }

    @Override
    public Optional<RoundUpSavingsSettingEntity> findRoundUpSavingsByAccount(MintAccountEntity mintAccount) {
        return repository.findTopByAccount(mintAccount);
    }

    @Override
    public Optional<RoundUpSavingsSettingEntity> findActiveRoundUpSavingsByUser(AppUserEntity user) {
        return repository.findTopByCreatorAndEnabledTrue(user);
    }

    @Override
    public List<RoundUpSavingsSettingEntity> getDeactivateSavingsWithZeroBalance(LocalDateTime deactivatedBeforeTime, int size) {
        Pageable pageable = PageRequest.of(0, size);
        return repository.getDeactivatedSavingsForDeletion(BigDecimal.ZERO, deactivatedBeforeTime, pageable);
    }
}
