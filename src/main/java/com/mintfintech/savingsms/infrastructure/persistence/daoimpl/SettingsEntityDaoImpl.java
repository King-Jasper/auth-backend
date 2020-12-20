package com.mintfintech.savingsms.infrastructure.persistence.daoimpl;

import com.mintfintech.savingsms.domain.dao.SettingsEntityDao;
import com.mintfintech.savingsms.domain.entities.SettingsEntity;
import com.mintfintech.savingsms.domain.entities.enums.SettingsNameTypeConstant;
import com.mintfintech.savingsms.infrastructure.persistence.repository.SettingsRepository;
import javax.inject.Named;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Wed, 16 Dec, 2020
 */
@Named
public class SettingsEntityDaoImpl implements SettingsEntityDao {

    private final SettingsRepository repository;
    public SettingsEntityDaoImpl(SettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    public String getSettings(SettingsNameTypeConstant name, String defaultValue) {
        Optional<SettingsEntity> optSettings = repository.findFirstByName(name);
        if(!optSettings.isPresent()) {
            SettingsEntity settingsEntity = SettingsEntity.builder()
                    .name(name).value(defaultValue).build();
            repository.saveAndFlush(settingsEntity);
            return defaultValue;
        }
        return optSettings.get().getValue();
    }
}
