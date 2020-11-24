package com.mintfintech.savingsms.usecase.master_record.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mintfintech.savingsms.domain.dao.SavingsGoalCategoryEntityDao;
import com.mintfintech.savingsms.domain.entities.SavingsGoalCategoryEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.usecase.master_record.SavingsGoalCategoryUseCase;
import com.mintfintech.savingsms.usecase.models.SavingsGoalCategoryModel;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jnwanya on
 * Wed, 01 Apr, 2020
 */
@AllArgsConstructor
@Named
public class SavingsGoalCategoryUseCaseImpl implements SavingsGoalCategoryUseCase {

    private final SavingsGoalCategoryEntityDao savingsGoalCategoryEntityDao;
    private final Gson gson;

    @Override
    public void createDefaultSavingsCategory() {
        /*if(savingsGoalCategoryEntityDao.countSavingsGoalCategory() != 0) {
            return;
        }*/
        InputStream inputStream = TypeReference.class.getResourceAsStream("/json/savings-goal-category.json");
        try {
            String fileContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            if(!StringUtils.isEmpty(fileContent)) {
                Type collectionType = new TypeToken<List<SavingsGoalCategoryModel>>(){}.getType();
                List<SavingsGoalCategoryModel> responseList = gson.fromJson(fileContent, collectionType);
                System.out.println("total savings goal category: " + responseList.size());
                responseList.forEach(savingsGoalCategoryModel -> {
                    if(!savingsGoalCategoryEntityDao.findCategoryByCode(savingsGoalCategoryModel.getCode()).isPresent()) {
                        SavingsGoalCategoryEntity categoryEntity = SavingsGoalCategoryEntity.builder()
                                .code(savingsGoalCategoryModel.getCode())
                                .name(savingsGoalCategoryModel.getName())
                                .build();
                        if("10".equalsIgnoreCase(savingsGoalCategoryModel.getCode())) {
                            categoryEntity.setRecordStatus(RecordStatusConstant.INACTIVE);
                        }
                        savingsGoalCategoryEntityDao.saveRecord(categoryEntity);
                    }
                });
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Unable to create saving goal category records: " + e.getMessage());
        }
    }

    @Override
    public List<SavingsGoalCategoryModel> savingsGoalCategoryList() {
        return savingsGoalCategoryEntityDao.getSavingsGoalCategoryList().stream()
                .map(categoryEntity -> SavingsGoalCategoryModel.builder()
                        .code(categoryEntity.getCode())
                        .name(categoryEntity.getName())
                        .build()).collect(Collectors.toList());
    }
}
