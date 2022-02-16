package com.mintfintech.savingsms.usecase.features.spend_and_save.impl;

import com.mintfintech.savingsms.domain.dao.SpendAndSaveTransactionDao;
import com.mintfintech.savingsms.domain.entities.SpendAndSaveTransactionEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.features.spend_and_save.GetSpendAndSaveTransactionUseCase;
import com.mintfintech.savingsms.usecase.models.SpendAndSaveTransactionModel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;

import javax.inject.Named;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;


@Named
@AllArgsConstructor
public class GetSpendAndSaveTransactionUseCaseImpl implements GetSpendAndSaveTransactionUseCase {

    private final SpendAndSaveTransactionDao spendAndSaveTransactionDao;

    @Override
    public PagedDataResponse<SpendAndSaveTransactionModel> getSpendAndSaveTransactions(SavingsGoalEntity savingsGoal) {
        Page<SpendAndSaveTransactionEntity> spendAndSaveTransactionPage = spendAndSaveTransactionDao.getTransactionsBySavingsGoal(savingsGoal, 0, 6);

        return new PagedDataResponse<>(spendAndSaveTransactionPage.getTotalElements(), spendAndSaveTransactionPage.getTotalPages(),
                spendAndSaveTransactionPage.stream().map(this::fromTransactionEntityToModel).collect(Collectors.toList()));
    }

    private SpendAndSaveTransactionModel fromTransactionEntityToModel(SpendAndSaveTransactionEntity transactionEntity) {
        return SpendAndSaveTransactionModel.builder()
                .transactionAmount(transactionEntity.getAmountSaved())
                .dateCreated(transactionEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .transactionType(transactionEntity.getTransactionType().name())
                .build();
    }
}
