package com.mintfintech.savingsms.usecase.features.corporate.impl;

import com.mintfintech.savingsms.domain.dao.*;
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

    @Override
    public CorporateInvestmentDetailResponse getInvestmentRequestDetail(AuthenticatedUser currentUser, String requestId) {
        CorporateTransactionRequestEntity requestEntity = getCorporateTransactionRequest(currentUser, requestId);

        AppUserEntity initiator = requestEntity.getInitiator();

        CorporateTransactionEntity transactionEntity = corporateTransactionEntityDao.getByTransactionRequest(requestEntity);

        InvestmentEntity investmentEntity = investmentEntityDao.getRecordById(transactionEntity.getTransactionRecordId());

        BigDecimal expectedReturns = getInvestmentUseCase.calculateTotalExpectedReturn(investmentEntity.getAmountInvested(), investmentEntity.getAccruedInterest(), investmentEntity.getInterestRate(), investmentEntity.getMaturityDate());

        return CorporateInvestmentDetailResponse.builder()
                .amount(investmentEntity.getAmountInvested())
                .approvalStatus(requestEntity.getApprovalStatus().name())
                .initiator(initiator.getName())
                .expectedReturns(expectedReturns)
                .maturityDate(investmentEntity.getMaturityDate().format(DateTimeFormatter.ISO_DATE))
                .transactionCategory(CorporateTransactionCategoryConstant.INVESTMENT.name())
                .interestRate(investmentEntity.getInterestRate())
                .dateInitiated(investmentEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();

    }

    @Override
    public CorporateInvestmentTopUpDetailResponse getInvestmentTopUpRequestDetail(AuthenticatedUser currentUser, String requestId) {

        CorporateTransactionRequestEntity requestEntity = getCorporateTransactionRequest(currentUser, requestId);

        CorporateTransactionEntity transactionEntity = corporateTransactionEntityDao.getByTransactionRequest(requestEntity);
        InvestmentEntity investmentEntity = investmentEntityDao.getRecordById(transactionEntity.getTransactionRecordId());

        BigDecimal toBeAmountInvested = investmentEntity.getAmountInvested().add(requestEntity.getTotalAmount());

        BigDecimal expectedInterest = getInvestmentUseCase.calculateOutstandingInterest(toBeAmountInvested, investmentEntity.getInterestRate(), investmentEntity.getMaturityDate());

        BigDecimal currentAccruedInterest = investmentEntity.getAccruedInterest();

        BigDecimal expectedReturns = toBeAmountInvested.add(expectedInterest).add(currentAccruedInterest);

        //getInvestmentUseCase.calculateTotalExpectedReturn(amount, investmentEntity.getAccruedInterest(), investmentEntity.getInterestRate(), investmentEntity.getMaturityDate());

        return CorporateInvestmentTopUpDetailResponse.builder()
                .approvalStatus(requestEntity.getApprovalStatus().name())
                .transactionCategory(CorporateTransactionCategoryConstant.INVESTMENT.name())
                .amountInvested(investmentEntity.getAmountInvested())
                .dateInitiated(investmentEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .initiator(investmentEntity.getCreator().getName())
                .interestRate(investmentEntity.getInterestRate())
                .investmentDuration(investmentEntity.getDurationInMonths())
                .maturityDate(investmentEntity.getMaturityDate().format(DateTimeFormatter.ISO_DATE))
                .topUpAmount(requestEntity.getTotalAmount())
                .interestAccrued(investmentEntity.getAccruedInterest().doubleValue())
                .totalExpectedReturns(expectedReturns)
                .build();
    }

    @Override
    public CorporateInvestmentLiquidationDetailResponse getInvestmentLiquidationRequestDetail(AuthenticatedUser currentUser, String requestId) {

        CorporateTransactionRequestEntity requestEntity = getCorporateTransactionRequest(currentUser, requestId);

        CorporateTransactionEntity transactionEntity = corporateTransactionEntityDao.getByTransactionRequest(requestEntity);
        InvestmentEntity investmentEntity = investmentEntityDao.getRecordById(transactionEntity.getTransactionRecordId());

        BigDecimal amount = investmentEntity.getAmountInvested();
        BigDecimal expectedReturns = BigDecimal.ZERO;

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            expectedReturns = getInvestmentUseCase.calculateTotalExpectedReturn(amount, investmentEntity.getAccruedInterest(), investmentEntity.getInterestRate(), investmentEntity.getMaturityDate());
        }

        return CorporateInvestmentLiquidationDetailResponse.builder()
                .transactionCategory(CorporateTransactionCategoryConstant.INVESTMENT.name())
                .amountInvested(investmentEntity.getAmountInvested())
                .dateInitiated(investmentEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .initiator(investmentEntity.getCreator().getName())
                .interestRate(investmentEntity.getInterestRate())
                .investmentDuration(investmentEntity.getDurationInMonths())
                .maturityDate(investmentEntity.getMaturityDate().format(DateTimeFormatter.ISO_DATE))
                .liquidationAmount(requestEntity.getTotalAmount())
                .interestAccrued(investmentEntity.getAccruedInterest().doubleValue())
                .totalExpectedReturns(expectedReturns)
                .approvalStatus(requestEntity.getApprovalStatus().name())
                .build();
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
