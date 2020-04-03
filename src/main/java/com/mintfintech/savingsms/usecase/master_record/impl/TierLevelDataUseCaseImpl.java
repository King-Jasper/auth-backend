package com.mintfintech.savingsms.usecase.master_record.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.mintfintech.savingsms.domain.dao.TierLevelEntityDao;
import com.mintfintech.savingsms.domain.entities.TierLevelEntity;
import com.mintfintech.savingsms.domain.entities.enums.TierLevelTypeConstant;
import com.mintfintech.savingsms.usecase.master_record.TierLevelDataUseCase;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
@Slf4j
@Named
public class TierLevelDataUseCaseImpl implements TierLevelDataUseCase {

    private TierLevelEntityDao tierLevelEntityDao;
    private Gson gson;

    public TierLevelDataUseCaseImpl(TierLevelEntityDao tierLevelEntityDao, Gson gson) {
        this.tierLevelEntityDao = tierLevelEntityDao;
        this.gson = gson;
    }

    @Override
    public void createDefaultTierLevels() {
        System.out.println("DEFAULT TIER LEVEL CALLED");
        if(tierLevelEntityDao.countRecords() != 0) {
            return;
        }
        InputStream inputStream = TypeReference.class.getResourceAsStream("/json/cbn-tier-levels.json");
        try {
            String fileContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            if(!StringUtils.isEmpty(fileContent)) {
                Type collectionType = new TypeToken<List<TierLevelData>>(){}.getType();
                List<TierLevelData> responseList = gson.fromJson(fileContent, collectionType);
                System.out.println("total tier levels: " + responseList.size());
                responseList.forEach(tierLevelData -> {
                    TierLevelEntity levelEntity = TierLevelEntity.builder()
                            .level(TierLevelTypeConstant.valueOf(tierLevelData.tier))
                            .maximumBalance(BigDecimal.valueOf(tierLevelData.maximumBalance))
                            .bulletTransactionAmount(BigDecimal.valueOf(tierLevelData.bulletTransaction))
                            .build();
                    tierLevelEntityDao.saveRecord(levelEntity);
                });
            }
        }catch (Exception e){
            e.printStackTrace();
            log.info("Unable to create tier levels");
        }
    }

    @Data
    private static class TierLevelData {
        private String tier;
        @SerializedName("bullet_transaction")
        private double bulletTransaction;
        @SerializedName("maximum_balance")
        private double maximumBalance;
    }
}
