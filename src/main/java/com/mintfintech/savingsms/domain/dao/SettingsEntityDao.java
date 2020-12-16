package com.mintfintech.savingsms.domain.dao;

import com.mintfintech.savingsms.domain.entities.enums.SettingsNameTypeConstant;

/**
 * Created by jnwanya on
 * Wed, 16 Dec, 2020
 */
public interface SettingsEntityDao  {
    String getSettings(SettingsNameTypeConstant name, String defaultValue);
}
