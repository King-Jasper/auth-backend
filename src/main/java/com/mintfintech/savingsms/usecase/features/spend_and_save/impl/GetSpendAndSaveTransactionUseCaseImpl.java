package com.mintfintech.savingsms.usecase.features.spend_and_save.impl;

import com.mintfintech.savingsms.domain.dao.SpendAndSaveTransactionEntity;
import com.mintfintech.savingsms.domain.dao.SpendAndSaveTransactionEntityDao;
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

    private final SpendAndSaveTransactionEntityDao spendAndSaveTransactionEntityDao;

    @Override
    public PagedDataResponse<SpendAndSaveTransactionModel> getSpendAndSaveTransactions(SavingsGoalEntity savingsGoal) {
        Page<SpendAndSaveTransactionEntity> spendAndSaveTransactionPage = spendAndSaveTransactionEntityDao.getTransactionsBySavingsGoal(savingsGoal, 1, 6);

        return new PagedDataResponse<>(spendAndSaveTransactionPage.getTotalElements(), spendAndSaveTransactionPage.getTotalElements(),
                spendAndSaveTransactionPage.stream().map(this::fromTransactionEntityToModel).collect(Collectors.toList()));
    }

    private SpendAndSaveTransactionModel fromTransactionEntityToModel(SpendAndSaveTransactionEntity transactionEntity) {
        return SpendAndSaveTransactionModel.builder()
                .amountSaved(transactionEntity.getAmountSaved())
                .dateCreated(transactionEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .transactionType(transactionEntity.getTransactionType().name())
                .build();
    }
}
