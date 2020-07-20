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
import com.mintfintech.savingsms.usecase.models.deprecated.SavingsPlanDModel;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
@Named
public class SavingsPlanUseCasesImpl implements SavingsPlanUseCases {

    private final SavingsPlanEntityDao savingsPlanEntityDao;
    private final SavingsPlanTenorEntityDao savingsPlanTenorEntityDao;
    private final Gson gson;

    public SavingsPlanUseCasesImpl(SavingsPlanEntityDao savingsPlanEntityDao, SavingsPlanTenorEntityDao savingsPlanTenorEntityDao, Gson gson) {
        this.savingsPlanEntityDao = savingsPlanEntityDao;
        this.savingsPlanTenorEntityDao = savingsPlanTenorEntityDao;
        this.gson = gson;
    }

    @Override
    public List<SavingsPlanModel> savingsPlanList() {
        return savingsPlanEntityDao.getSavingsPlans().stream().map(this::fromEntityToModel).collect(Collectors.toList());
    }

    @Deprecated
    @Override
    public List<SavingsPlanDModel> savingsPlanDeprecatedList() {
        return savingsPlanEntityDao.getSavingsPlans().stream().map(this::fromEntityToOldModel).collect(Collectors.toList());
    }

    @Override
    public List<SavingsPlanTenorModel> savingsTenorList() {
        return savingsPlanTenorEntityDao.getTenorList().stream()
                .filter(savingsPlanTenorEntity ->  savingsPlanTenorEntity.getMaximumDuration() > 0)
                .map(savingsPlanTenorEntity -> SavingsPlanTenorModel.builder()
                        .durationId(savingsPlanTenorEntity.getId())
                        .description(savingsPlanTenorEntity.getDurationDescription())
                        .value(savingsPlanTenorEntity.getDuration())
                        .interestRate(savingsPlanTenorEntity.getInterestRate())
                        .maximumDuration(savingsPlanTenorEntity.getMaximumDuration())
                        .minimumDuration(savingsPlanTenorEntity.getMinimumDuration())
                        .build()
                ).sorted(Comparator.comparing(SavingsPlanTenorModel::getValue))
                .collect(Collectors.toList());
    }

    private SavingsPlanDModel fromEntityToOldModel(SavingsPlanEntity savingsPlanEntity) {
        List<SavingsPlanTenorModel> tenorModelList = savingsPlanTenorEntityDao.getTenorListByPlan(savingsPlanEntity).stream()
                .map(savingsPlanTenorEntity -> SavingsPlanTenorModel.builder()
                        .durationId(savingsPlanTenorEntity.getId())
                        .description(savingsPlanTenorEntity.getDurationDescription())
                        .value(savingsPlanTenorEntity.getDuration())
                        .interestRate(savingsPlanTenorEntity.getInterestRate())
                        .build()
                ).sorted(Comparator.comparing(SavingsPlanTenorModel::getValue))
                .collect(Collectors.toList());

        return SavingsPlanDModel.builder()
                .planId(savingsPlanEntity.getPlanId())
                .maximumBalance(savingsPlanEntity.getMaximumBalance())
                .minimumBalance(savingsPlanEntity.getMinimumBalance())
                .name(savingsPlanEntity.getPlanName().getName())
                .interestRate(0.0)
                .durations(tenorModelList)
                .build();
    }

    private SavingsPlanModel fromEntityToModel(SavingsPlanEntity savingsPlanEntity) {
        return SavingsPlanModel.builder()
                .planId(savingsPlanEntity.getPlanId())
                .maximumBalance(savingsPlanEntity.getMaximumBalance())
                .minimumBalance(savingsPlanEntity.getMinimumBalance())
                .name(savingsPlanEntity.getPlanName().getName())
                .build();
    }

    @Override
    public void createDefaultSavingsPlan() {
        if(savingsPlanEntityDao.countSavingPlans() != 0) {
           return;
        }
        InputStream inputStream = TypeReference.class.getResourceAsStream("/json/saving-plans_v1.json");
        try {
            String fileContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            if(!StringUtils.isEmpty(fileContent)) {
                Type collectionType = new TypeToken<List<SavingsPlanData>>(){}.getType();
                List<SavingsPlanData> responseList = gson.fromJson(fileContent, collectionType);
                System.out.println("total savings plan: " + responseList.size());
                responseList.forEach(this::createOrUpdateSavingsPlan);
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Unable to saving plans records: " + e.getMessage());
        }
    }

    @Override
    public void createDefaultSavingsTenor() {
        /*if(savingsPlanTenorEntityDao.countSavingsPlanTenor() != 0) {
            return;
        }*/
        InputStream inputStream = TypeReference.class.getResourceAsStream("/json/savings-tenors.json");
        try {
            String fileContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            if(!StringUtils.isEmpty(fileContent)) {
                Type collectionType = new TypeToken<List<SavingsPlanDataTenor>>(){}.getType();
                List<SavingsPlanDataTenor> responseList = gson.fromJson(fileContent, collectionType);
                System.out.println("total savings tenor: " + responseList.size());
                responseList.forEach(this::createOrUpdateSavingsTenor);
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Unable to saving plans records: " + e.getMessage());
        }
    }

    private void createOrUpdateSavingsTenor(SavingsPlanDataTenor planDataTenor)  {
        Optional<SavingsPlanTenorEntity> planTenorOptional = savingsPlanTenorEntityDao.findSavingPlanTenor(planDataTenor.minimumDuration, planDataTenor.maximumDuration);
        SavingsPlanTenorEntity planTenor = planTenorOptional.orElseGet(SavingsPlanTenorEntity::new);
        planTenor.setDuration(planDataTenor.minimumDuration);
        planTenor.setMinimumDuration(planDataTenor.minimumDuration);
        planTenor.setMaximumDuration(planDataTenor.maximumDuration);
        planTenor.setDurationType(SavingsDurationTypeConstant.valueOf(planDataTenor.durationType));
        planTenor.setInterestRate(planDataTenor.interestRate);
        planTenor.setDurationDescription(planDataTenor.description);
        savingsPlanTenorEntityDao.saveRecord(planTenor);
    }

    private void createOrUpdateSavingsPlan(SavingsPlanData savingsPlanData) {
        SavingsPlanTypeConstant planType = SavingsPlanTypeConstant.valueOf(savingsPlanData.name);
        Optional<SavingsPlanEntity> planEntityOptional = savingsPlanEntityDao.findBPlanByType(planType);
        SavingsPlanEntity planEntity = planEntityOptional.orElseGet(SavingsPlanEntity::new);
        planEntity.setPlanName(planType);
        planEntity.setDescription("");
       // planEntity.setInterestRate(savingsPlanData.interestRate);
        planEntity.setMaximumBalance(BigDecimal.valueOf(savingsPlanData.maximumBalance));
        planEntity.setMinimumBalance(BigDecimal.valueOf(savingsPlanData.minimumBalance));
        if(StringUtils.isEmpty(planEntity.getPlanId())){
            planEntity.setPlanId(savingsPlanEntityDao.generatePlanId());
        }
        planEntity = savingsPlanEntityDao.saveRecord(planEntity);
    }

    @Data
    private static class SavingsPlanData {
        private String name;
        private double minimumBalance;
        private double maximumBalance;
       // private double interestRate;
       // private List<SavingsPlanDataTenor> tenors;
    }
    @Data
    private static class SavingsPlanDataTenor {
        private String durationType;
        private String description;
        private double interestRate;
        private int minimumDuration;
        private int maximumDuration;
    }
}
