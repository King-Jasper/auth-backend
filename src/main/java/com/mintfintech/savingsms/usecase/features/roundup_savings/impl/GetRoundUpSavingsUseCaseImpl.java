package com.mintfintech.savingsms.usecase.features.roundup_savings.impl;

import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.RoundUpSavingsSettingEntityDao;
import com.mintfintech.savingsms.domain.dao.RoundUpSavingsTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.RoundUpSavingsSettingEntity;
import com.mintfintech.savingsms.domain.entities.RoundUpSavingsTransactionEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.GetSavingsGoalUseCase;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.data.response.RoundUpSavingResponse;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.features.roundup_savings.GetRoundUpSavingsUseCase;
import com.mintfintech.savingsms.usecase.models.RoundUpSavingsTransactionModel;
import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import lombok.AllArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;

import javax.inject.Named;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by jnwanya on
 * Sat, 31 Oct, 2020
 */
@Named
@AllArgsConstructor
public class GetRoundUpSavingsUseCaseImpl implements GetRoundUpSavingsUseCase {

    private final GetSavingsGoalUseCase getSavingsGoalUseCase;
    private final RoundUpSavingsSettingEntityDao roundUpSavingsSettingEntityDao;
    private final RoundUpSavingsTransactionEntityDao roundUpSavingsTransactionEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;

    @Override
    public RoundUpSavingResponse fromEntityToResponse(RoundUpSavingsSettingEntity roundUpSavingsSettingEntity) {
        if(Hibernate.isInitialized(roundUpSavingsSettingEntity)) {
            roundUpSavingsSettingEntity = roundUpSavingsSettingEntityDao.getRecordById(roundUpSavingsSettingEntity.getId());
        }
        if(roundUpSavingsSettingEntity.getRoundUpSavings() == null) {
            return RoundUpSavingResponse.builder().exist(false).build();
        }
        SavingsGoalModel savingsGoalModel = getSavingsGoalUseCase.fromSavingsGoalEntityToModel(roundUpSavingsSettingEntity.getRoundUpSavings());
        return RoundUpSavingResponse.builder()
                .exist(true)
                .id(roundUpSavingsSettingEntity.getId())
                .isActive(roundUpSavingsSettingEntity.isEnabled())
                .roundUpType(roundUpSavingsSettingEntity.getFundTransferRoundUpType().getName())
                .savingsGoal(savingsGoalModel)
                .build();
    }

    @Override
    public RoundUpSavingResponse getAccountRoundUpSavings(AuthenticatedUser authenticatedUser) {
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        Optional<RoundUpSavingsSettingEntity> optional = roundUpSavingsSettingEntityDao.findRoundUpSavingsByAccount(accountEntity);
        if(!optional.isPresent()) {
            return RoundUpSavingResponse.builder().exist(false).build();
        }
        RoundUpSavingsSettingEntity roundUpSavingsSetting = optional.get();
        if(roundUpSavingsSetting.getRoundUpSavings() == null) {
            return RoundUpSavingResponse.builder().exist(false).build();
        }
        SavingsGoalEntity goalEntity = roundUpSavingsSetting.getRoundUpSavings();
        if(goalEntity.getRecordStatus() != RecordStatusConstant.ACTIVE) {
            return RoundUpSavingResponse.builder().exist(false).build();
        }
        if(goalEntity.getGoalStatus() == SavingsGoalStatusConstant.COMPLETED || goalEntity.getGoalStatus() == SavingsGoalStatusConstant.WITHDRAWN) {
            return RoundUpSavingResponse.builder().exist(false).build();
        }
        return fromEntityToResponse(roundUpSavingsSetting);
    }

    @Override
    public PagedDataResponse<RoundUpSavingsTransactionModel> getRoundUpSavingsTransaction(AuthenticatedUser authenticatedUser, Long roundUpId, int pageIndex, int size) {

        RoundUpSavingsSettingEntity roundUpSavings = roundUpSavingsSettingEntityDao.findById(roundUpId).orElseThrow(() -> new NotFoundException("Invalid roundup savings Id"));
        Page<RoundUpSavingsTransactionEntity> transactionEntityPage = roundUpSavingsTransactionEntityDao.getSuccessfulTransactionOnGoal(roundUpSavings.getRoundUpSavings(), pageIndex, size);
        return new PagedDataResponse<>(transactionEntityPage.getTotalElements(), transactionEntityPage.getTotalElements(),
                transactionEntityPage.stream().map(this::fromTransactionEntityToModel).collect(Collectors.toList()));
    }

    private RoundUpSavingsTransactionModel fromTransactionEntityToModel(RoundUpSavingsTransactionEntity transactionEntity) {
        return RoundUpSavingsTransactionModel.builder()
                .amountSaved(transactionEntity.getAmountSaved())
                .dateCreated(transactionEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .transactionAmount(transactionEntity.getTransactionAmount())
                .transactionType(transactionEntity.getTransactionType().name())
                .build();
    }
}
