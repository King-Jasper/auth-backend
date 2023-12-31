package com.mintfintech.savingsms.usecase.master_record.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mintfintech.savingsms.domain.dao.InvestmentTenorEntityDao;
import com.mintfintech.savingsms.domain.entities.InvestmentTenorEntity;
import com.mintfintech.savingsms.domain.entities.enums.SavingsDurationTypeConstant;
import com.mintfintech.savingsms.usecase.master_record.InvestmentPlanUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentTenorModel;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class InvestmentPlanUseCaseImpl implements InvestmentPlanUseCase {

    private final Gson gson;
    private final InvestmentTenorEntityDao investmentTenorEntityDao;

    @Override
    public void createDefaultInvestmentTenor() {
        InputStream inputStream = TypeReference.class.getResourceAsStream("/json/investment-tenors.json");
        try {
            String fileContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            if (!StringUtils.isEmpty(fileContent)) {
                Type collectionType = new TypeToken<List<InvestmentPlanUseCaseImpl.InvestmentTenor>>() {
                }.getType();
                List<InvestmentPlanUseCaseImpl.InvestmentTenor> responseList = gson.fromJson(fileContent, collectionType);
                System.out.println("total investment tenor: " + responseList.size());
                responseList.forEach(this::createOrUpdateInvestmentTenor);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unable to save investment tenor records: " + e.getMessage());
        }
    }

    @Override
    public List<InvestmentTenorModel> investmentTenorList() {
        return investmentTenorEntityDao.getTenorList().stream()
                .filter(investmentTenorEntity ->  investmentTenorEntity.getMaximumDuration() > 0)
                .map(investmentTenorEntity -> InvestmentTenorModel.builder()
                        .durationId(investmentTenorEntity.getId())
                        .description(investmentTenorEntity.getDurationDescription())
                        .interestRate(investmentTenorEntity.getInterestRate())
                        .maximumDuration(investmentTenorEntity.getMaximumDuration())
                        .minimumDuration(investmentTenorEntity.getMinimumDuration())
                        .penaltyRate(investmentTenorEntity.getPenaltyRate())
                        .build()
                ).sorted(Comparator.comparing(InvestmentTenorModel::getMinimumDuration))
                .collect(Collectors.toList());
    }

    private void createOrUpdateInvestmentTenor(InvestmentPlanUseCaseImpl.InvestmentTenor tenor) {
        Optional<InvestmentTenorEntity> planTenorOptional = investmentTenorEntityDao.findInvestmentTenor(tenor.minimumDuration, tenor.maximumDuration);
        log.info("min {} max - {} exist - {}", tenor.minimumDuration, tenor.maximumDuration, planTenorOptional.isPresent());
        InvestmentTenorEntity planTenor = planTenorOptional.orElseGet(InvestmentTenorEntity::new);
        planTenor.setMinimumDuration(tenor.minimumDuration);
        planTenor.setMaximumDuration(tenor.maximumDuration);
        planTenor.setDurationType(SavingsDurationTypeConstant.valueOf(tenor.durationType));
        planTenor.setInterestRate(tenor.interestRate);
        planTenor.setDurationDescription(tenor.description);
        planTenor.setPenaltyRate(tenor.penaltyRate);
        investmentTenorEntityDao.saveRecord(planTenor);
    }

    @Data
    private static class InvestmentTenor {
        private String durationType;
        private String description;
        private double interestRate;
        private int minimumDuration;
        private int maximumDuration;
        private double penaltyRate;
    }
}
