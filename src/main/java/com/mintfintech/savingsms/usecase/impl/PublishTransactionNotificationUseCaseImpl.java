package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalEntity;
import com.mintfintech.savingsms.domain.entities.SavingsGoalTransactionEntity;
import com.mintfintech.savingsms.domain.entities.enums.TransactionTypeConstant;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.usecase.PublishTransactionNotificationUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.MintTransactionEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.PushNotificationEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalFundingEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalFundingFailureEvent;
import com.mintfintech.savingsms.utils.MoneyFormatterUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.springframework.scheduling.annotation.Async;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by jnwanya on
 * Sat, 24 Oct, 2020
 */
@Slf4j
@Named
@AllArgsConstructor
public class PublishTransactionNotificationUseCaseImpl implements PublishTransactionNotificationUseCase {

    private final SavingsGoalEntityDao savingsGoalEntityDao;
    private final SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    private final ApplicationEventService applicationEventService;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final AppUserEntityDao appUserEntityDao;

    @Async
    @Override
    public void createTransactionLog(SavingsGoalTransactionEntity savingsGoalTransactionEntity, BigDecimal openingBalance, BigDecimal currentBalance) {
        /*if(!Hibernate.isInitialized(savingsGoalTransactionEntity)) {
            savingsGoalTransactionEntity = savingsGoalTransactionEntityDao.getRecordById(savingsGoalTransactionEntity.getId());
        }*/
        try {
            Thread.sleep(3000);
        }catch (Exception ignored){ }
        savingsGoalTransactionEntity = savingsGoalTransactionEntityDao.getRecordById(savingsGoalTransactionEntity.getId());
        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.getRecordById(savingsGoalTransactionEntity.getBankAccount().getId());
        SavingsGoalEntity savingsGoalEntity = savingsGoalEntityDao.getRecordById(savingsGoalTransactionEntity.getSavingsGoal().getId());
        String description = "Savings Goal funding - "+savingsGoalEntity.getGoalId()+"|"+savingsGoalEntity.getName();
        MintTransactionEvent transactionPayload = MintTransactionEvent.builder()
                .balanceAfterTransaction(currentBalance)
                .balanceBeforeTransaction(openingBalance)
                .transactionAmount(savingsGoalTransactionEntity.getTransactionAmount())
                .transactionType(TransactionTypeConstant.DEBIT.name())
                .category("SAVINGS_GOAL")
                .debitAccountId(debitAccount.getAccountId())
                .description(description)
                .externalReference(savingsGoalTransactionEntity.getExternalReference())
                .internalReference(savingsGoalTransactionEntity.getTransactionReference())
                .spendingTagId(0)
                .dateCreated(savingsGoalTransactionEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.MINT_TRANSACTION_LOG, new EventModel<>(transactionPayload));
    }

    @Async
    @Override
    public void sendSavingsFundingSuccessNotification(SavingsGoalTransactionEntity transactionEntity) {
        if(!Hibernate.isInitialized(transactionEntity)) {
            transactionEntity = savingsGoalTransactionEntityDao.getRecordById(transactionEntity.getId());
        }
        BigDecimal savingsAmount = transactionEntity.getTransactionAmount();
        SavingsGoalEntity goalEntity = savingsGoalEntityDao.getRecordById(transactionEntity.getSavingsGoal().getId());
        AppUserEntity appUserEntity = appUserEntityDao.getRecordById(goalEntity.getCreator().getId());
        if(appUserEntity.isEmailNotificationEnabled() && !StringUtils.isEmpty(appUserEntity.getEmail())){
            SavingsGoalFundingEvent fundingEvent = SavingsGoalFundingEvent.builder()
                    .amount(savingsAmount)
                    .goalName(goalEntity.getName())
                    .reference(transactionEntity.getTransactionReference())
                    .name(appUserEntity.getName())
                    .recipient(appUserEntity.getEmail())
                    .transactionDate(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                    .savingsBalance(goalEntity.getSavingsBalance())
                    .build();
            applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_SAVINGS_GOAL_FUNDING_SUCCESS, new EventModel<>(fundingEvent));
        }else {
            log.info("Email notification disabled: {}", appUserEntity.getEmail());
        }
        if(appUserEntity.isGcmNotificationEnabled()) {
            if("".equalsIgnoreCase(appUserEntity.getDeviceGcmNotificationToken())) {
                //device token does not exist for user. probably a web user.
                return;
            }
            String text = String.format("Congrats, you just saved N%s in your savings goal(%s)", MoneyFormatterUtil.priceWithoutDecimal(savingsAmount), goalEntity.getName());
            PushNotificationEvent pushNotificationEvent = new PushNotificationEvent("New Savings", text, appUserEntity.getDeviceGcmNotificationToken());
            pushNotificationEvent.setUserId(appUserEntity.getUserId());
            if(appUserEntity.getDeviceGcmNotificationToken() == null) {
                //accounts will publish the device token value if it exist.
                applicationEventService.publishEvent(ApplicationEventService.EventType.PUSH_NOTIFICATION_TOKEN_ACCOUNTS, new EventModel<>(pushNotificationEvent));
                appUserEntity.setDeviceGcmNotificationToken("");
                appUserEntityDao.saveRecord(appUserEntity);
            }else {
                applicationEventService.publishEvent(ApplicationEventService.EventType.PUSH_NOTIFICATION_TOKEN, new EventModel<>(pushNotificationEvent));
            }
        }
    }

    @Async
    @Override
    public void sendSavingsFundingFailureNotification(SavingsGoalEntity goalEntity, BigDecimal savingsAmount, String failureMessage) {
        AppUserEntity appUserEntity = appUserEntityDao.getRecordById(goalEntity.getCreator().getId());
        if(!appUserEntity.isEmailNotificationEnabled()){
            log.info("Email notification disabled: {}", appUserEntity.getEmail());
            return;
        }
        SavingsGoalFundingFailureEvent failureEvent = SavingsGoalFundingFailureEvent.builder()
                .failureMessage(failureMessage)
                .amount(savingsAmount)
                .goalName(goalEntity.getName())
                .status("FAILED")
                .name(appUserEntity.getName())
                .transactionDate(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .recipient(appUserEntity.getEmail()).build();
        applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_SAVINGS_GOAL_FUNDING_FAILURE, new EventModel<>(failureEvent));
    }
}
