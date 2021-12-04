package com.mintfintech.savingsms.usecase.features.corporate.impl;

import com.google.gson.Gson;
import com.mintfintech.savingsms.domain.dao.CorporateTransactionEntityDao;
import com.mintfintech.savingsms.domain.dao.CorporateTransactionRequestEntityDao;
import com.mintfintech.savingsms.domain.dao.InvestmentEntityDao;
import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.CorporateTransactionCategoryConstant;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.CorporateInvestmentDetailResponse;
import com.mintfintech.savingsms.usecase.data.response.CorporateInvestmentLiquidationDetailResponse;
import com.mintfintech.savingsms.usecase.data.response.CorporateInvestmentTopUpDetailResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.UnauthorisedException;
import com.mintfintech.savingsms.usecase.features.corporate.GetCorporateTransactionUseCase;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentDetailsInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Slf4j
@AllArgsConstructor
public class GetCorporateTransactionUseCaseImpl implements GetCorporateTransactionUseCase {

    private final MintAccountEntityDao mintAccountEntityDao;
    private final CorporateTransactionRequestEntityDao corporateTransactionRequestEntityDao;
    private final CorporateTransactionEntityDao corporateTransactionEntityDao;
    private final InvestmentEntityDao investmentEntityDao;
    private final GetInvestmentUseCase getInvestmentUseCase;
    private final Gson gson;

    @Override
    public CorporateInvestmentDetailResponse getInvestmentRequestDetail(AuthenticatedUser currentUser, String requestId) {
        CorporateTransactionRequestEntity requestEntity = getCorporateTransactionRequest(currentUser, requestId);

        AppUserEntity initiator = requestEntity.getInitiator();

        CorporateTransactionEntity transactionEntity = corporateTransactionEntityDao.getByTransactionRequest(requestEntity);

        InvestmentEntity investmentEntity = investmentEntityDao.getRecordById(transactionEntity.getTransactionRecordId());

        BigDecimal amountInvested = investmentEntity.getAmountInvested();
        BigDecimal expectedReturns;
        if(transactionEntity.getTransactionMetaData() != null) {
            InvestmentDetailsInfo investmentDetailsInfo = gson.fromJson(transactionEntity.getTransactionMetaData(), InvestmentDetailsInfo.class);
            amountInvested = investmentDetailsInfo.getAmountInvested();
            expectedReturns = investmentDetailsInfo.getTotalExpectedReturns();
        }else {
            expectedReturns = getInvestmentUseCase.calculateTotalExpectedReturn(investmentEntity.getAmountInvested(), investmentEntity.getAccruedInterest(), investmentEntity.getInterestRate(), investmentEntity.getMaturityDate());
        }
        CorporateInvestmentDetailResponse response = CorporateInvestmentDetailResponse.builder()
                .requestId(requestId)
                .amount(amountInvested)
                .investmentDuration(investmentEntity.getDurationInMonths())
                .approvalStatus(requestEntity.getApprovalStatus().name())
                .initiator(initiator.getName())
                .expectedReturns(expectedReturns)
                .maturityDate(investmentEntity.getMaturityDate().format(DateTimeFormatter.ISO_DATE_TIME))
                .transactionCategory(CorporateTransactionCategoryConstant.INVESTMENT.name())
                .interestRate(investmentEntity.getInterestRate())
                .dateInitiated(investmentEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();

        if(requestEntity.getDateReviewed() != null) {
            response.setDateReviewed(requestEntity.getDateReviewed().format(DateTimeFormatter.ISO_DATE_TIME));
            response.setReviewedBy(requestEntity.getReviewer().getName());
        }

        return response;

    }

    @Override
    public CorporateInvestmentTopUpDetailResponse getInvestmentTopUpRequestDetail(AuthenticatedUser currentUser, String requestId) {

        CorporateTransactionRequestEntity requestEntity = getCorporateTransactionRequest(currentUser, requestId);

        CorporateTransactionEntity transactionEntity = corporateTransactionEntityDao.getByTransactionRequest(requestEntity);
        InvestmentEntity investmentEntity = investmentEntityDao.getRecordById(transactionEntity.getTransactionRecordId());

        //BigDecimal toBeAmountInvested = investmentEntity.getAmountInvested().add(requestEntity.getTotalAmount());

        //BigDecimal expectedInterest = getInvestmentUseCase.calculateOutstandingInterest(toBeAmountInvested, investmentEntity.getInterestRate(), investmentEntity.getMaturityDate());

        //BigDecimal currentAccruedInterest = investmentEntity.getAccruedInterest();

        //BigDecimal expectedReturns = toBeAmountInvested.add(expectedInterest).add(currentAccruedInterest);

        //getInvestmentUseCase.calculateTotalExpectedReturn(amount, investmentEntity.getAccruedInterest(), investmentEntity.getInterestRate(), investmentEntity.getMaturityDate());

        BigDecimal amountInvested, accruedInterest, expectedReturns;
        if(transactionEntity.getTransactionMetaData() != null) {
            InvestmentDetailsInfo investmentDetailsInfo = gson.fromJson(transactionEntity.getTransactionMetaData(), InvestmentDetailsInfo.class);
            amountInvested = investmentDetailsInfo.getAmountInvested();
            accruedInterest = investmentDetailsInfo.getInterestAccrued();
            expectedReturns = investmentDetailsInfo.getTotalExpectedReturns();

        }else {
            amountInvested = investmentEntity.getAmountInvested();
            accruedInterest = investmentEntity.getAccruedInterest();

            BigDecimal toBeAmountInvested = investmentEntity.getAmountInvested().add(requestEntity.getTotalAmount());

            BigDecimal expectedInterest = getInvestmentUseCase.calculateOutstandingInterest(toBeAmountInvested, investmentEntity.getInterestRate(), investmentEntity.getMaturityDate());

            BigDecimal currentAccruedInterest = investmentEntity.getAccruedInterest();

            expectedReturns = toBeAmountInvested.add(expectedInterest).add(currentAccruedInterest);

            //getInvestmentUseCase.calculateTotalExpectedReturn(amount, investmentEntity.getAccruedInterest(), investmentEntity.getInterestRate(), investmentEntity.getMaturityDate());
        }

        CorporateInvestmentTopUpDetailResponse response = CorporateInvestmentTopUpDetailResponse.builder()
                .requestId(requestId)
                .approvalStatus(requestEntity.getApprovalStatus().name())
                .transactionCategory(CorporateTransactionCategoryConstant.INVESTMENT.name())
                .amountInvested(amountInvested)
                .dateInitiated(investmentEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .initiator(investmentEntity.getCreator().getName())
                .interestRate(investmentEntity.getInterestRate())
                .investmentDuration(investmentEntity.getDurationInMonths())
                .maturityDate(investmentEntity.getMaturityDate().format(DateTimeFormatter.ISO_DATE_TIME))
                .topUpAmount(requestEntity.getTotalAmount())
                .interestAccrued(accruedInterest)
                .totalExpectedReturns(expectedReturns)
                .build();

        if(requestEntity.getDateReviewed() != null) {
            response.setDateReviewed(requestEntity.getDateReviewed().format(DateTimeFormatter.ISO_DATE_TIME));
            response.setReviewedBy(requestEntity.getReviewer().getName());
        }

        return response;
    }

    @Override
    public CorporateInvestmentLiquidationDetailResponse getInvestmentLiquidationRequestDetail(AuthenticatedUser currentUser, String requestId) {

        CorporateTransactionRequestEntity requestEntity = getCorporateTransactionRequest(currentUser, requestId);

        CorporateTransactionEntity transactionEntity = corporateTransactionEntityDao.getByTransactionRequest(requestEntity);
        InvestmentEntity investmentEntity = investmentEntityDao.getRecordById(transactionEntity.getTransactionRecordId());

        BigDecimal amountInvested, accruedInterest, expectedReturns;
        if(transactionEntity.getTransactionMetaData() != null) {
            InvestmentDetailsInfo investmentDetailsInfo = gson.fromJson(transactionEntity.getTransactionMetaData(), InvestmentDetailsInfo.class);

            amountInvested = investmentDetailsInfo.getAmountInvested();
            accruedInterest = investmentDetailsInfo.getInterestAccrued();
            expectedReturns = investmentDetailsInfo.getTotalExpectedReturns();

        }else {
            amountInvested = investmentEntity.getAmountInvested();
            accruedInterest = investmentEntity.getAccruedInterest();
            expectedReturns = getInvestmentUseCase.calculateTotalExpectedReturn(investmentEntity.getAmountInvested(), investmentEntity.getAccruedInterest(), investmentEntity.getInterestRate(), investmentEntity.getMaturityDate());
        }

        CorporateInvestmentLiquidationDetailResponse response = CorporateInvestmentLiquidationDetailResponse.builder()
                .requestId(requestId)
                .transactionCategory(CorporateTransactionCategoryConstant.INVESTMENT.name())
                .amountInvested(amountInvested)
                .dateInitiated(investmentEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .initiator(investmentEntity.getCreator().getName())
                .interestRate(investmentEntity.getInterestRate())
                .investmentDuration(investmentEntity.getDurationInMonths())
                .maturityDate(investmentEntity.getMaturityDate().format(DateTimeFormatter.ISO_DATE_TIME))
                .liquidationAmount(requestEntity.getTotalAmount())
                .interestAccrued(accruedInterest.doubleValue())
                .totalExpectedReturns(expectedReturns)
                .approvalStatus(requestEntity.getApprovalStatus().name())
                .build();

        if(requestEntity.getDateReviewed() != null) {
            response.setDateReviewed(requestEntity.getDateReviewed().format(DateTimeFormatter.ISO_DATE_TIME));
            response.setReviewedBy(requestEntity.getReviewer().getName());
        }

        return response;
    }

    private CorporateTransactionRequestEntity getCorporateTransactionRequest(AuthenticatedUser currentUser, String requestId) {
        MintAccountEntity mintAccountEntity = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());

        Optional<CorporateTransactionRequestEntity> requestEntityOpt = corporateTransactionRequestEntityDao.findByRequestId(requestId);

        if (!requestEntityOpt.isPresent()) {
            throw new BadRequestException("Invalid request Id.");
        }

        CorporateTransactionRequestEntity requestEntity = requestEntityOpt.get();
        if (!requestEntity.getCorporate().getId().equals(mintAccountEntity.getId())) {
            log.info("Request Id does not belong to same corporate account");
            throw new UnauthorisedException("Request aborted.");
        }

        if (requestEntity.getTransactionCategory() != CorporateTransactionCategoryConstant.INVESTMENT) {
            throw new BusinessLogicConflictException("Sorry, transaction record does not exist on this service.");
        }
        return requestEntity;
    }
}
