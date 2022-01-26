package com.mintfintech.savingsms.usecase.features.spend_and_save.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.dao.SpendAndSaveEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SpendAndSaveEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.SavingsGoalStatusConstant;
import com.mintfintech.savingsms.domain.services.AuditTrailService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.SpendAndSaveSetUpRequest;
import com.mintfintech.savingsms.usecase.data.response.SpendAndSaveResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.features.spend_and_save.GetSpendAndSaveTransactionUseCase;
import com.mintfintech.savingsms.usecase.features.spend_and_save.UpdateSpendAndSaveUseCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import javax.inject.Named;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Named
@AllArgsConstructor
@Slf4j
public class UpdateSpendAndSaveUseCaseImpl implements UpdateSpendAndSaveUseCase {

    private final MintAccountEntityDao mintAccountEntityDao;
    private final AppUserEntityDao appUserEntityDao;
    private final SpendAndSaveEntityDao spendAndSaveEntityDao;
    private final AuditTrailService auditTrailService;
    private final GetSpendAndSaveTransactionUseCase getSpendAndSaveTransactionUseCase;
    private final SavingsGoalEntityDao savingsGoalEntityDao;

    @Override
    public SpendAndSaveResponse updateSpendAndSaveStatus(AuthenticatedUser authenticatedUser, boolean statusValue) {
        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());

        Optional<SpendAndSaveEntity> spendAndSaveOptional = spendAndSaveEntityDao.findSpendAndSaveSettingByAccount(mintAccount);
        if(!spendAndSaveOptional.isPresent()) {
            throw new BadRequestException("No Spend and Save setting exist for user.");
        }
        SpendAndSaveEntity spendAndSave = spendAndSaveOptional.get();

        SavingsGoalEntity goalEntity = spendAndSave.getSavings();
        if(goalEntity.getRecordStatus() != RecordStatusConstant.ACTIVE) {
            throw new BadRequestException("Sorry, savings is not active.");
        }

        SpendAndSaveEntity oldState = new SpendAndSaveEntity();
        BeanUtils.copyProperties(spendAndSave, oldState);
        String oldStatus = spendAndSave.isActivated() ? "ACTIVE" : "INACTIVE";
        String newStatus = statusValue ? "ACTIVE" : "INACTIVE";

        spendAndSave.setActivated(statusValue);
        if(statusValue) {
            spendAndSave.setDateActivated(LocalDateTime.now());
        }else {
            spendAndSave.setDateDeactivated(LocalDateTime.now());
        }
        spendAndSaveEntityDao.saveRecord(spendAndSave);

        String description = String.format("Spend and Save Status update: From %s to  %s on record %d.", oldStatus, newStatus, spendAndSave.getId());
        auditTrailService.createAuditLog(authenticatedUser, AuditTrailService.AuditType.UPDATE, description, spendAndSave, oldState);
        return SpendAndSaveResponse.builder()
                .exist(true)
                .accruedInterest(goalEntity.getAccruedInterest())
                .maturityDate(StringUtils.defaultString(goalEntity.getMaturityDate().format(DateTimeFormatter.ISO_DATE_TIME), ""))
                .amountSaved(goalEntity.getSavingsBalance())
                .status(goalEntity.getGoalStatus().name())
                .savings(getSpendAndSaveTransactionUseCase.getSpendAndSaveTransactions(goalEntity))
                .build();
    }

    @Override
    public String editSpendAndSaveSettings(AuthenticatedUser authenticatedUser, SpendAndSaveSetUpRequest request) {

        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());
        int percentage = request.getTransactionPercentage();
        if (percentage < 1) {
            throw new BadRequestException("Minimum required percentage is 1");
        }

        Optional<SpendAndSaveEntity> spendAndSaveOptional = spendAndSaveEntityDao.findSpendAndSaveByAppUserAndMintAccount(appUser, mintAccount);
        if (!spendAndSaveOptional.isPresent()) {
            throw new BadRequestException("Spend and save record not found");
        }
        SpendAndSaveEntity spendAndSave = spendAndSaveOptional.get();
        spendAndSave.setPercentage(percentage);
        spendAndSaveEntityDao.saveRecord(spendAndSave);
        return "Your savings plan changed successfully";
    }
}
