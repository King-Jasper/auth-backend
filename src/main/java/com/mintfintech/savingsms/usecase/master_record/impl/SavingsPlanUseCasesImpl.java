package com.mintfintech.savingsms.usecase.master_record.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mintfintech.savingsms.domain.dao.SavingsPlanEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsPlanTenorEntityDao;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.usecase.master_record.SavingsPlanUseCases;
import com.mintfintech.savingsms.usecase.models.SavingsPlanModel;
import com.mintfintech.savingsms.usecase.models.SavingsPlanTenorModel;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
@Named
public class SavingsPlanUseCasesImpl implements SavingsPlanUseCases {

    private SavingsPlanEntityDao savingsPlanEntityDao;
    private SavingsPlanTenorEntityDao savingsPlanTenorEntityDao;
    private Gson gson;

    public SavingsPlanUseCasesImpl(SavingsPlanEntityDao savingsPlanEntityDao, SavingsPlanTenorEntityDao savingsPlanTenorEntityDao, Gson gson) {
        this.savingsPlanEntityDao = savingsPlanEntityDao;
        this.savingsPlanTenorEntityDao = savingsPlanTenorEntityDao;
        this.gson = gson;
    }

    @Override
    public List<SavingsPlanModel> savingsPlanList() {
        return savingsPlanEntityDao.getSavingsPlans().stream().map(this::fromEntityToModel).collect(Collectors.toList());
    }


    private SavingsPlanModel fromEntityToModel(SavingsPlanEntity savingsPlanEntity) {
        List<SavingsPlanTenorModel> tenorModelList = savingsPlanTenorEntityDao.getTenorListByPlan(savingsPlanEntity).stream()
                .map(savingsPlanTenorEntity -> SavingsPlanTenorModel.builder()
                        .durationId(savingsPlanTenorEntity.getId())
                        .description(String.format("%d Days", savingsPlanTenorEntity.getDuration()))
                        .value(savingsPlanTenorEntity.getDuration())
                        .build()
                ).collect(Collectors.toList());

        return SavingsPlanModel.builder()
                .planId(savingsPlanEntity.getPlanId())
                .maximumBalance(savingsPlanEntity.getMaximumBalance())
                .minimumBalance(savingsPlanEntity.getMinimumBalance())
                .name(savingsPlanEntity.getPlanName().getName())
                .interestRate(savingsPlanEntity.getInterestRate())
                .durations(tenorModelList)
                .build();
    }

    @Override
    public void createDefaultSavingsPlan() {
        if(savingsPlanEntityDao.countSavingPlans() != 0) {
            return;
        }
        InputStream inputStream = TypeReference.class.getResourceAsStream("/json/saving-plans.json");
        try {
            String fileContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            if(!StringUtils.isEmpty(fileContent)) {
                Type collectionType = new TypeToken<List<SavingsPlanData>>(){}.getType();
                List<SavingsPlanData> responseList = gson.fromJson(fileContent, collectionType);
                System.out.println("total savings plan: " + responseList.size());
                responseList.forEach(this::createSavingsPlan);
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Unable to saving plans records: " + e.getMessage());
        }
    }

    private void createSavingsPlan(SavingsPlanData savingsPlanData) {
        SavingsPlanEntity savingsPlanEntity = SavingsPlanEntity.builder()
                .planName(SavingsPlanTypeConstant.valueOf(savingsPlanData.name))
                .description("").interestRate(savingsPlanData.interestRate)
                .maximumBalance(BigDecimal.valueOf(savingsPlanData.maximumBalance))
                .minimumBalance(BigDecimal.valueOf(savingsPlanData.minimumBalance))
                .planId(savingsPlanEntityDao.generatePlanId()).build();
        savingsPlanEntityDao.saveRecord(savingsPlanEntity);
        savingsPlanData.getTenors().forEach(savingsPlanDataTenor -> {
            SavingsPlanTenorEntity planTenorEntity = SavingsPlanTenorEntity.builder()
                    .durationType(SavingsDurationTypeConstant.valueOf(savingsPlanDataTenor.durationType))
                    .savingsPlan(savingsPlanEntity).duration(savingsPlanDataTenor.duration)
                    .build();
            savingsPlanTenorEntityDao.saveRecord(planTenorEntity);
        });
    }

    @Data
    private static class SavingsPlanData {
        private String name;
        private double minimumBalance;
        private double maximumBalance;
        private double interestRate;
        private List<SavingsPlanDataTenor> tenors;
    }
    @Data
    private static class SavingsPlanDataTenor {
        private String durationType;
        private int duration;
    }
}
