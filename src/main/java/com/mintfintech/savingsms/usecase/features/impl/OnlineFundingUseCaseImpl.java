package com.mintfintech.savingsms.usecase.features.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.corebankingservice.GeneratedReferenceCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.SavingsFundingReferenceRequestCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.SavingsFundingVerificationResponseCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.events.incoming.OnlinePaymentStatusUpdateEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SavingsGoalFundingEvent;
import com.mintfintech.savingsms.usecase.data.request.OnlineFundingRequest;
import com.mintfintech.savingsms.usecase.data.response.OnlineFundingResponse;
import com.mintfintech.savingsms.usecase.data.response.ReferenceGenerationResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.NotFoundException;
import com.mintfintech.savingsms.usecase.features.OnlineFundingUseCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import javax.inject.Named;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Thu, 22 Oct, 2020
 */
@Slf4j
@Named
@AllArgsConstructor
public class OnlineFundingUseCaseImpl implements OnlineFundingUseCase {

    private final CoreBankingServiceClient coreBankingServiceClient;
    private final SavingsFundingRequestEntityDao savingsFundingRequestEntityDao;
    private final SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    private final SavingsGoalEntityDao savingsGoalEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final AppUserEntityDao appUserEntityDao;

    @Override
    public ReferenceGenerationResponse createFundingRequest(AuthenticatedUser authenticatedUser, OnlineFundingRequest fundingRequest) {
        AppUserEntity currentUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());
        if(fundingRequest.getAmount() < 100) {
            throw new BadRequestException("Sorry, minimum amount that can be funded is N100.00");
        }
        BigDecimal amount =  BigDecimal.valueOf(fundingRequest.getAmount());
        SavingsGoalEntity savingsGoal = savingsGoalEntityDao.findSavingGoalByGoalId(fundingRequest.getGaolId()).orElseThrow(() -> new BadRequestException("Invalid goal Id."));

        BigDecimal newBalance = amount.add(savingsGoal.getSavingsBalance());
        PaymentGatewayTypeConstant gatewayType = PaymentGatewayTypeConstant.valueOf(fundingRequest.getPaymentGateway());

        String fundingReference = savingsGoalTransactionEntityDao.generateTransactionReference();
        String paymentReference = generateTransactionReference(savingsGoal.getGoalId(), fundingReference, fundingRequest);

        SavingsFundingRequestEntity entity = SavingsFundingRequestEntity.builder()
                .amount(amount)
                .fundingReference(fundingReference)
                .paymentReference(paymentReference)
                .paymentGateway(gatewayType)
                .savingsGoal(savingsGoal)
                .paymentStatus(TransactionStatusConstant.PENDING)
                .creator(currentUser)
                .build();
        savingsFundingRequestEntityDao.saveRecord(entity);

        if(gatewayType  == PaymentGatewayTypeConstant.PAYSTACK) {
            amount = entity.getAmount().add(computeTransactionFee(entity.getAmount()));
        }
        return ReferenceGenerationResponse.builder()
                .transactionReference(paymentReference)
                .amount(amount)
                .build();
    }

    @Override
    public OnlineFundingResponse verifyFundingRequest(AuthenticatedUser authenticatedUser, String reference) {

        Optional<SavingsFundingRequestEntity> savingsFundingRequestEntityOptional = savingsFundingRequestEntityDao.findByPaymentReference(reference);
        if(!savingsFundingRequestEntityOptional.isPresent()) {
            throw new NotFoundException("Transaction with reference "+reference+" not found.");
        }
        SavingsFundingRequestEntity savingsFundingRequestEntity = savingsFundingRequestEntityOptional.get();
        SavingsGoalEntity savingsGoalEntity = savingsGoalEntityDao.getRecordById(savingsFundingRequestEntity.getSavingsGoal().getId());
        if(savingsFundingRequestEntity.getPaymentStatus() == TransactionStatusConstant.PENDING) {
            updatePaymentStatus(savingsFundingRequestEntity);
        }else {
            log.info("PAYMENT STATUS ALREADY UPDATED BEFORE VERIFICATION");
        }
        savingsFundingRequestEntity = savingsFundingRequestEntityDao.getRecordById(savingsFundingRequestEntity.getId());
        if(savingsFundingRequestEntity.getPaymentStatus() == TransactionStatusConstant.SUCCESSFUL && savingsFundingRequestEntity.getFundingTransaction() == null) {
            updateSavingsBalance(savingsFundingRequestEntity);
        }
        String transactionDate;
        if(savingsFundingRequestEntity.getPaymentDate() != null) {
            transactionDate = savingsFundingRequestEntity.getPaymentDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }else {
            transactionDate = savingsFundingRequestEntity.getDateModified().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        return OnlineFundingResponse.builder()
                .goalId(savingsGoalEntity.getGoalId())
                .amount(savingsFundingRequestEntity.getAmount())
                .paymentGateway(savingsFundingRequestEntity.getPaymentGateway().name())
                .paymentStatus(savingsFundingRequestEntity.getPaymentStatus().name())
                .transactionReference(savingsFundingRequestEntity.getPaymentReference())
                .transactionDate(transactionDate)
                .build();
    }

    @Override
    public void updateFundingPaymentStatus(OnlinePaymentStatusUpdateEvent paymentStatusUpdateEvent) {
        String reference = paymentStatusUpdateEvent.getTransactionReference();
        Optional<SavingsFundingRequestEntity> savingsFundingRequestEntityOptional = savingsFundingRequestEntityDao.findByPaymentReference(reference);
        if(!savingsFundingRequestEntityOptional.isPresent()) {
            return;
        }
        SavingsFundingRequestEntity fundingRequestEntity = savingsFundingRequestEntityOptional.get();
        if(fundingRequestEntity.getPaymentStatus() != TransactionStatusConstant.PENDING) {
            return;
        }
        TransactionStatusConstant paymentStatus = TransactionStatusConstant.valueOf(paymentStatusUpdateEvent.getPaymentStatus());
        fundingRequestEntity.setPaymentStatus(paymentStatus);
        if(!StringUtils.isEmpty(paymentStatusUpdateEvent.getPaymentDate())) {
            fundingRequestEntity.setPaymentDate(LocalDateTime.parse(paymentStatusUpdateEvent.getPaymentDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        savingsFundingRequestEntityDao.saveRecord(fundingRequestEntity);
        updateSavingsBalance(fundingRequestEntity);
    }

    private void updateSavingsBalance(SavingsFundingRequestEntity fundingRequestEntity) {
        if(fundingRequestEntity.getPaymentStatus() == TransactionStatusConstant.SUCCESSFUL && fundingRequestEntity.getFundingTransaction() == null) {
            SavingsGoalEntity savingsGoalEntity = savingsGoalEntityDao.getRecordById(fundingRequestEntity.getSavingsGoal().getId());
            BigDecimal currentBalance = savingsGoalEntity.getSavingsBalance();
            BigDecimal newBalance = currentBalance.add(fundingRequestEntity.getAmount());
            AppUserEntity appUserEntity = appUserEntityDao.getRecordById(fundingRequestEntity.getCreator().getId());
            MintAccountEntity accountEntity = mintAccountEntityDao.getRecordById(appUserEntity.getPrimaryAccount().getId());
            MintBankAccountEntity bankAccountEntity = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(accountEntity, BankAccountTypeConstant.CURRENT);

            SavingsGoalTransactionEntity transactionEntity = new SavingsGoalTransactionEntity();
            transactionEntity.setSavingsGoal(savingsGoalEntity);
            transactionEntity.setCurrentBalance(currentBalance);
            transactionEntity.setBankAccount(bankAccountEntity);
            transactionEntity.setTransactionAmount(fundingRequestEntity.getAmount());
            transactionEntity.setFundingSource(FundingSourceTypeConstant.CARD);
            transactionEntity.setNewBalance(newBalance);
            transactionEntity.setTransactionReference(StringUtils.defaultString(fundingRequestEntity.getFundingReference(), fundingRequestEntity.getPaymentReference()));
            transactionEntity.setTransactionResponseMessage("Success");
            transactionEntity.setTransactionResponseCode("00");
            transactionEntity.setTransactionType(TransactionTypeConstant.CREDIT);
            transactionEntity.setTransactionStatus(TransactionStatusConstant.SUCCESSFUL);

            savingsGoalTransactionEntityDao.saveRecord(transactionEntity);
            fundingRequestEntity.setFundingTransaction(transactionEntity);
            savingsFundingRequestEntityDao.saveRecord(fundingRequestEntity);
            savingsGoalEntity.setSavingsBalance(newBalance);
            savingsGoalEntityDao.saveRecord(savingsGoalEntity);
        }
    }

    private String generateTransactionReference(String goalId, String fundingReference, OnlineFundingRequest fundingRequest) {
        SavingsFundingReferenceRequestCBS requestCBS = SavingsFundingReferenceRequestCBS.builder()
                .goalId(goalId)
                .fundingReference(fundingReference)
                .amountInNaira(fundingRequest.getAmount())
                .paymentGateway(fundingRequest.getPaymentGateway())
                .build();
        MsClientResponse<GeneratedReferenceCBS> msClientResponse = coreBankingServiceClient.generateSavingsFundingReference(requestCBS);
        if(!msClientResponse.isSuccess()){
            String message = StringUtils.isEmpty(msClientResponse.getMessage())? "Sorry, request could not be completed at the moment." : msClientResponse.getMessage();
            throw new BusinessLogicConflictException(message);
        }
        return msClientResponse.getData().getReference();
    }

    private BigDecimal computeTransactionFee(BigDecimal fundingAmount) {
       /* if(!applicationProperty.isProductionEnvironment()) {
            return BigDecimal.ZERO;
        }*/
        BigDecimal payStackCharge = BigDecimal.valueOf(0.985);
        BigDecimal payStackFixedCharge = BigDecimal.valueOf(100.00);
        BigDecimal transactionFee = BigDecimal.ZERO;

        if (fundingAmount.compareTo(BigDecimal.valueOf(2500)) < 0 ){
            transactionFee =fundingAmount.divide(payStackCharge,2, RoundingMode.HALF_UP).subtract(fundingAmount);
        }

        else if(fundingAmount.compareTo(BigDecimal.valueOf(2500)) >= 0){
            BigDecimal totalAmount = fundingAmount.add(payStackFixedCharge).divide(payStackCharge,2, RoundingMode.HALF_UP);
            transactionFee = totalAmount.subtract(fundingAmount);
        }

        if(transactionFee.compareTo(BigDecimal.valueOf(2000)) > 0){
            transactionFee = BigDecimal.valueOf(2000);
        }
        return transactionFee.setScale(2, RoundingMode.HALF_EVEN);
    }

    private void updatePaymentStatus(SavingsFundingRequestEntity fundingRequestEntity) {
        MsClientResponse<SavingsFundingVerificationResponseCBS> msClientResponse = coreBankingServiceClient.verifySavingsFundingRequest(fundingRequestEntity.getPaymentReference());
        if(msClientResponse.isSuccess()) {
            SavingsFundingVerificationResponseCBS fundingReferenceResponseCBS = msClientResponse.getData();
            fundingRequestEntity.setAmount(BigDecimal.valueOf(fundingReferenceResponseCBS.getAmount()));
            fundingRequestEntity.setPaymentStatus(TransactionStatusConstant.valueOf(fundingReferenceResponseCBS.getPaymentStatus()));
            if(!StringUtils.isEmpty(fundingReferenceResponseCBS.getPaymentDate())) {
                fundingRequestEntity.setPaymentDate(LocalDateTime.parse(fundingReferenceResponseCBS.getPaymentDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            fundingRequestEntity = savingsFundingRequestEntityDao.saveRecord(fundingRequestEntity);
        }
    }
}
