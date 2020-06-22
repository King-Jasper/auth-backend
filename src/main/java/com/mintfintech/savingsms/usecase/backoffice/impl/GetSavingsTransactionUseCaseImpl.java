package com.mintfintech.savingsms.usecase.backoffice.impl;

import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.models.reports.SavingsMaturityStat;
import com.mintfintech.savingsms.usecase.backoffice.GetSavingsTransactionUseCase;
import com.mintfintech.savingsms.usecase.data.response.SavingsMaturityStatModel;
import com.mintfintech.savingsms.usecase.data.response.SavingsMaturityStatSummary;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.inject.Named;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Mon, 22 Jun, 2020
 */

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Named
@AllArgsConstructor
public class GetSavingsTransactionUseCaseImpl implements GetSavingsTransactionUseCase {

    SavingsGoalEntityDao savingsGoalEntityDao;

    @Override
    public SavingsMaturityStatSummary getSavingsMaturityStatistics(LocalDate fromDate, LocalDate toDate) {
        long days = fromDate.until(toDate, ChronoUnit.DAYS);
        if(days > 90) {
            throw new BusinessLogicConflictException("Sorry, Maximum of 90 days range is permitted");
        }
        if(fromDate.isBefore(LocalDate.now())) {
            throw new BusinessLogicConflictException("Start date cannot be before current date.");
        }
        LocalDateTime startDate = fromDate.atStartOfDay();
        LocalDateTime endDate = toDate.atTime(LocalTime.MAX);
        List<SavingsMaturityStat> maturityStatList = savingsGoalEntityDao.savingsMaturityStatisticsList(startDate, endDate);
        SavingsMaturityStatSummary maturityStatModel = new SavingsMaturityStatSummary();
        List<SavingsMaturityStatModel> savingsMaturityStatList = new ArrayList<>();
        for(int i = 0; i <= days; i++) {
            final int day = fromDate.getDayOfMonth();
            final int month = fromDate.getMonthValue();
            SavingsMaturityStatModel maturityStat = new SavingsMaturityStatModel();
            maturityStat.setMaturityDate(fromDate.format(DateTimeFormatter.ISO_DATE));
            Optional<SavingsMaturityStat> statOptional = maturityStatList.stream().filter(modelStat -> month == modelStat.getMonth() && day == modelStat.getDay()).findFirst();
            statOptional.ifPresent(modelStat -> {
                maturityStat.setTotalRecords(modelStat.getTotalRecords());
                maturityStat.setTotalAmount(modelStat.getTotalInterest().add(modelStat.getTotalSavings()));
                maturityStat.setTotalInterest(modelStat.getTotalInterest());
                maturityStat.setTotalSavings(modelStat.getTotalSavings());
                maturityStatModel.setTotalAmount(modelStat.getTotalInterest().add(modelStat.getTotalSavings()));
                maturityStatModel.setTotalInterest(maturityStatModel.getTotalInterest().add(modelStat.getTotalInterest()));
                maturityStatModel.setTotalSavings(maturityStatModel.getTotalSavings().add(modelStat.getTotalSavings()));
                maturityStatModel.setTotalSavingsRecord(maturityStatModel.getTotalSavingsRecord() + modelStat.getTotalRecords());
            });
            savingsMaturityStatList.add(maturityStat);
            fromDate = fromDate.plusDays(1);
        }
        maturityStatModel.setSavingsMaturityStatList(savingsMaturityStatList);
        return maturityStatModel;
    }


}
