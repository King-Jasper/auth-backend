package com.mintfintech.savingsms.usecase.features.corporate.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.CorporateTransactionEntity;
import com.mintfintech.savingsms.domain.entities.CorporateTransactionRequestEntity;
import com.mintfintech.savingsms.domain.entities.InvestmentEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.CorporateTransactionCategoryConstant;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.CorporateInvestmentDetailResponse;
import com.mintfintech.savingsms.usecase.data.response.CorporateInvestmentTopUpDetailResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.UnauthorisedException;
import com.mintfintech.savingsms.usecase.features.corporate.GetCorporateTransactionUseCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    @Override
    public CorporateInvestmentDetailResponse getInvestmentRequestDetail(AuthenticatedUser currentUser, String requestId) {
        MintAccountEntity mintAccountEntity = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());

        Optional<CorporateTransactionRequestEntity> requestEntityOpt = corporateTransactionRequestEntityDao.findByRequestId(requestId);

        if(!requestEntityOpt.isPresent()) {
            throw new BadRequestException("Invalid request Id.");
        }

        CorporateTransactionRequestEntity requestEntity = requestEntityOpt.get();
        if(!requestEntity.getCorporate().getId().equals(mintAccountEntity.getId())) {
            log.info("Request Id does not belong to same corporate account");
            throw new UnauthorisedException("Request aborted.");
        }

        if(requestEntity.getTransactionCategory() != CorporateTransactionCategoryConstant.INVESTMENT) {
            throw new BusinessLogicConflictException("Sorry, transaction record does not exist on this service.");
        }

        CorporateTransactionEntity transactionEntity = corporateTransactionEntityDao.getByTransactionRequest(requestEntity);

        InvestmentEntity investmentEntity = investmentEntityDao.getRecordById(transactionEntity.getTransactionRecordId());
        return CorporateInvestmentDetailResponse.builder()
                .amount(investmentEntity.getTotalAmountInvested().doubleValue())
                .maturityDate(investmentEntity.getMaturityDate().format(DateTimeFormatter.ofPattern("MMM, dd yyyy")))
                .transactionCategory(CorporateTransactionCategoryConstant.INVESTMENT.name())
                .interestRate(investmentEntity.getInterestRate())
                .dateInitiated(investmentEntity.getDateCreated().format(DateTimeFormatter.ofPattern("MMM, dd yyyy")))
                .build();

    }

    @Override
    public CorporateInvestmentTopUpDetailResponse getInvestmentTopUpRequestDetail(AuthenticatedUser currentUser, String requestId) {

        MintAccountEntity mintAccountEntity = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());

        Optional<CorporateTransactionRequestEntity> requestEntityOpt = corporateTransactionRequestEntityDao.findByRequestId(requestId);

        if(!requestEntityOpt.isPresent()) {
            throw new BadRequestException("Invalid request Id.");
        }

        CorporateTransactionRequestEntity requestEntity = requestEntityOpt.get();
        if(!requestEntity.getCorporate().getId().equals(mintAccountEntity.getId())) {
            log.info("Request Id does not belong to same corporate account");
            throw new UnauthorisedException("Request aborted.");
        }

        if(requestEntity.getTransactionCategory() != CorporateTransactionCategoryConstant.INVESTMENT) {
            throw new BusinessLogicConflictException("Sorry, transaction record does not exist on this service.");
        }

        CorporateTransactionEntity transactionEntity = corporateTransactionEntityDao.getByTransactionRequest(requestEntity);

        InvestmentEntity investmentEntity = investmentEntityDao.getRecordById(transactionEntity.getTransactionRecordId());
        return CorporateInvestmentTopUpDetailResponse.builder()
                .transactionCategory(CorporateTransactionCategoryConstant.INVESTMENT.name())
                .initialAmount(investmentEntity.getAmountInvested())
                .dateInitiated(investmentEntity.getDateCreated().format(DateTimeFormatter.ofPattern("MMM, dd yyyy")))
                .initiator(investmentEntity.getCreator().getName())
                .interestRate(investmentEntity.getInterestRate())
                .investmentDuration(investmentEntity.getDurationInMonths())
                .maturityDate(investmentEntity.getMaturityDate().format(DateTimeFormatter.ofPattern("MMM, dd yyyy")))
                .topUpAmount(requestEntity.getTotalAmount())
                .interestAccrued(investmentEntity.getAccruedInterest().doubleValue())
                .build();
    }
}
