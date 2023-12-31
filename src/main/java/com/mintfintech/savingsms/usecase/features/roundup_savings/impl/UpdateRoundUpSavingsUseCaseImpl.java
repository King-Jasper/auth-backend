package com.mintfintech.savingsms.usecase.features.roundup_savings.impl;

import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.RoundUpSavingsSettingEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.RoundUpSavingsSettingEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.enums.RecordStatusConstant;
import com.mintfintech.savingsms.domain.entities.enums.RoundUpSavingsTypeConstant;
import com.mintfintech.savingsms.domain.services.AuditTrailService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.RoundUpTypeUpdateRequest;
import com.mintfintech.savingsms.usecase.data.response.RoundUpSavingResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.features.roundup_savings.GetRoundUpSavingsUseCase;
import com.mintfintech.savingsms.usecase.features.roundup_savings.UpdateRoundUpSavingsUseCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Sat, 31 Oct, 2020
 */
@Slf4j
@AllArgsConstructor
@Named
public class UpdateRoundUpSavingsUseCaseImpl implements UpdateRoundUpSavingsUseCase {

    private final GetRoundUpSavingsUseCase getRoundUpSavingsUseCase;
    private final RoundUpSavingsSettingEntityDao roundUpSavingsSettingEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final AuditTrailService auditTrailService;
    private final SavingsGoalEntityDao savingsGoalEntityDao;

    @Override
    public RoundUpSavingResponse updateRoundUpType(AuthenticatedUser authenticatedUser, Long roundUpSetUpId, RoundUpTypeUpdateRequest request) {
        RoundUpSavingsTypeConstant fundTransferRPSavingsType = RoundUpSavingsTypeConstant.valueOf(request.getFundTransferRoundUpType());
        RoundUpSavingsTypeConstant billPaymentRPSavingsType = RoundUpSavingsTypeConstant.valueOf(request.getBillPaymentRoundUpType());

        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());

        Optional<RoundUpSavingsSettingEntity> opt = roundUpSavingsSettingEntityDao.findById(roundUpSetUpId);
        if(!opt.isPresent()) {
            throw new BadRequestException("Invalid roundup savings Id");
        }
        RoundUpSavingsSettingEntity roundUpSavingsSetting = opt.get();
        if(!roundUpSavingsSetting.getAccount().getId().equals(accountEntity.getId())) {
            log.info("RoundUpSavingsSettingEntity {} does not belong to account {}", roundUpSetUpId, accountEntity.getId());
            throw new BadRequestException("Invalid roundup savings Id");
        }

        SavingsGoalEntity goalEntity = roundUpSavingsSetting.getRoundUpSavings();
        if(goalEntity.getRecordStatus() != RecordStatusConstant.ACTIVE) {
            throw new BadRequestException("Sorry, roundup savings is not active.");
        }


        RoundUpSavingsSettingEntity oldState = new RoundUpSavingsSettingEntity();
        BeanUtils.copyProperties(roundUpSavingsSetting, oldState);
        String oldType1 = roundUpSavingsSetting.getFundTransferRoundUpType().getName();
        String oldType2 = roundUpSavingsSetting.getBillPaymentRoundUpType().getName();

        roundUpSavingsSetting.setBillPaymentRoundUpType(billPaymentRPSavingsType);
        roundUpSavingsSetting.setCardPaymentRoundUpType(RoundUpSavingsTypeConstant.NONE);
        roundUpSavingsSetting.setFundTransferRoundUpType(fundTransferRPSavingsType);
        roundUpSavingsSettingEntityDao.saveRecord(roundUpSavingsSetting);

        String description = String.format("RoundUp Type update: Fund Transfer update from %s to %s, Bill payment update from %s to %s on record %d",
                oldType1, fundTransferRPSavingsType.getName(), oldType2, billPaymentRPSavingsType.getName(), roundUpSavingsSetting.getId());
        auditTrailService.createAuditLog(authenticatedUser, AuditTrailService.AuditType.UPDATE, description, roundUpSavingsSetting, oldState);
        return getRoundUpSavingsUseCase.fromEntityToResponse(roundUpSavingsSetting);
    }

    @Override
    public RoundUpSavingResponse updateRoundUpSavingsStatus(AuthenticatedUser authenticatedUser, Long roundUpSetUpId, boolean active) {

        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());

        Optional<RoundUpSavingsSettingEntity> opt = roundUpSavingsSettingEntityDao.findById(roundUpSetUpId);
        if(!opt.isPresent()) {
            throw new BadRequestException("Invalid roundup savings Id");
        }
        RoundUpSavingsSettingEntity roundUpSavingsSetting = opt.get();
        if(!roundUpSavingsSetting.getAccount().getId().equals(accountEntity.getId())) {
            log.info("RoundUpSavingsSettingEntity {} does not belong to account {}", roundUpSetUpId, accountEntity.getId());
            throw new BadRequestException("Invalid roundup savings Id");
        }

        SavingsGoalEntity goalEntity = roundUpSavingsSetting.getRoundUpSavings();
        if(goalEntity.getRecordStatus() != RecordStatusConstant.ACTIVE) {
            throw new BadRequestException("Sorry, roundup savings is not active.");
        }

        RoundUpSavingsSettingEntity oldState = new RoundUpSavingsSettingEntity();
        BeanUtils.copyProperties(roundUpSavingsSetting, oldState);
        String oldStatus = roundUpSavingsSetting.isEnabled() ? "ACTIVE" : "INACTIVE";
        String newStatus = active ? "ACTIVE" : "INACTIVE";

        roundUpSavingsSetting.setEnabled(active);
        if(active) {
            roundUpSavingsSetting.setDateActivated(LocalDateTime.now());
        }else {
            roundUpSavingsSetting.setDateDeactivated(LocalDateTime.now());
        }
        roundUpSavingsSettingEntityDao.saveRecord(roundUpSavingsSetting);

        String description = String.format("RoundUp Saving Status update: From %s to  %s on record %d.", oldStatus, newStatus, roundUpSavingsSetting.getId());
        auditTrailService.createAuditLog(authenticatedUser, AuditTrailService.AuditType.UPDATE, description, roundUpSavingsSetting, oldState);
        return getRoundUpSavingsUseCase.fromEntityToResponse(roundUpSavingsSetting);
    }


    @Override
    public void deleteDeactivatedRoundUpSavingsWithZeroBalance() {
        int size = 50;
        LocalDateTime deactivated7DaysAgo = LocalDateTime.now().minusDays(7);
        List<RoundUpSavingsSettingEntity> settingEntityList = roundUpSavingsSettingEntityDao.getDeactivateSavingsWithZeroBalance(deactivated7DaysAgo, size);

        for(RoundUpSavingsSettingEntity savingsSettingEntity : settingEntityList) {
            if(savingsSettingEntity.isEnabled()) {
                continue;
            }
            SavingsGoalEntity savingsGoalEntity = savingsSettingEntity.getRoundUpSavings();
            if(savingsGoalEntity.getRecordStatus() != RecordStatusConstant.ACTIVE) {
                continue;
            }
            boolean zeroBalance = savingsGoalEntity.getSavingsBalance().compareTo(BigDecimal.ZERO) == 0;
            if(!zeroBalance) {
                continue;
            }
            savingsGoalEntity.setRecordStatus(RecordStatusConstant.DELETED);
            savingsSettingEntity.setRoundUpSavings(null);
            roundUpSavingsSettingEntityDao.saveRecord(savingsSettingEntity);
            savingsGoalEntityDao.deleteSavings(savingsGoalEntity);
            roundUpSavingsSettingEntityDao.deleteRecord(savingsSettingEntity);
        }
    }
}
