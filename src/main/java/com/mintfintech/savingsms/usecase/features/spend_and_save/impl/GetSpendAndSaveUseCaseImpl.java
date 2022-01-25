package com.mintfintech.savingsms.usecase.features.spend_and_save.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.SpendAndSaveEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SpendAndSaveEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.SpendAndSaveResponse;
import com.mintfintech.savingsms.usecase.features.spend_and_save.GetSpendAndSaveTransactionUseCase;
import com.mintfintech.savingsms.usecase.features.spend_and_save.GetSpendAndSaveUseCase;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Named;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Named
@AllArgsConstructor
public class GetSpendAndSaveUseCaseImpl implements GetSpendAndSaveUseCase {

    private final MintAccountEntityDao mintAccountEntityDao;
    private final AppUserEntityDao appUserEntityDao;
    private final SpendAndSaveEntityDao spendAndSaveEntityDao;
    private final GetSpendAndSaveTransactionUseCase getSpendAndSaveTransactionUseCase;


    @Override
    public SpendAndSaveResponse getSpendAndSaveDashboard(AuthenticatedUser authenticatedUser) {
        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        Optional<SpendAndSaveEntity> spendAndSaveOptional = spendAndSaveEntityDao.findSpendAndSaveByAppUserAndMintAccount(appUser, mintAccount);
        if (!spendAndSaveOptional.isPresent()) {
            return SpendAndSaveResponse.builder().exist(false).build();
        }
        SpendAndSaveEntity spendAndSaveEntity = spendAndSaveOptional.get();
        if(spendAndSaveEntity.getSavings() == null) {
            return SpendAndSaveResponse.builder().exist(false).build();
        }
        SavingsGoalEntity goalEntity = spendAndSaveEntity.getSavings();
        if(goalEntity.getRecordStatus() != RecordStatusConstant.ACTIVE) {
            return SpendAndSaveResponse.builder().exist(false).build();
        }
        if(goalEntity.getGoalStatus() == SavingsGoalStatusConstant.COMPLETED || goalEntity.getGoalStatus() == SavingsGoalStatusConstant.WITHDRAWN) {
            return SpendAndSaveResponse.builder().exist(false).build();
        }

        return SpendAndSaveResponse.builder()
                .exist(true)
                .accruedInterest(goalEntity.getAccruedInterest())
                .maturityDate(StringUtils.defaultString(goalEntity.getMaturityDate().format(DateTimeFormatter.ISO_DATE_TIME), ""))
                .amountSaved(goalEntity.getSavingsBalance())
                .status(goalEntity.getGoalStatus().name())
                .savings(getSpendAndSaveTransactionUseCase.getSpendAndSaveTransactions(goalEntity))
                .build();
    }

}
