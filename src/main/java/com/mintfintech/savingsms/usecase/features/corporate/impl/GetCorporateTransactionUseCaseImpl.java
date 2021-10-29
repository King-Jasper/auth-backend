package com.mintfintech.savingsms.usecase.features.corporate.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.AccountTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.CorporateTransactionCategoryConstant;
import com.mintfintech.savingsms.domain.entities.enums.CorporateTransactionTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionApprovalStatusConstant;
import com.mintfintech.savingsms.domain.models.reports.CorporateTransactionSearchDTO;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.CorporateTransactionSearchRequest;
import com.mintfintech.savingsms.usecase.data.response.CorporateTransactionDetailResponse;
import com.mintfintech.savingsms.usecase.data.response.PagedDataResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.UnauthorisedException;
import com.mintfintech.savingsms.usecase.features.corporate.GetCorporateTransactionUseCase;
import com.mintfintech.savingsms.usecase.models.CorporateTransactionRequestModel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class GetCorporateTransactionUseCaseImpl implements GetCorporateTransactionUseCase {

    private final MintAccountEntityDao mintAccountEntityDao;
    private final CorporateTransactionRequestEntityDao corporateTransactionRequestEntityDao;
    private final CorporateTransactionEntityDao corporateTransactionEntityDao;
    private final InvestmentEntityDao investmentEntityDao;
    private final AppUserEntityDao appUserEntityDao;

    @Override
    public CorporateTransactionDetailResponse getTransactionRequestDetail(AuthenticatedUser currentUser, String requestId) {
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

        return CorporateTransactionDetailResponse.builder()
                .amount(investmentEntity.getTotalAmountInvested().doubleValue())
                .maturityDate(investmentEntity.getMaturityDate().format(DateTimeFormatter.ofPattern("MMM, dd yyyy")))
                .transactionCategory(CorporateTransactionCategoryConstant.INVESTMENT.name())
                .interestRate(investmentEntity.getInterestRate())
                .dateInitiated(investmentEntity.getDateCreated().format(DateTimeFormatter.ofPattern("MMM, dd yyyy")))
                .build();

    }

    @Override
    public PagedDataResponse<CorporateTransactionRequestModel> getTransactionRequest(AuthenticatedUser currentUser, CorporateTransactionSearchRequest searchRequest, int page, int size) {
        MintAccountEntity mintAccountEntity = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());

        if(mintAccountEntity.getAccountType() != AccountTypeConstant.ENTERPRISE) {
            throw new BusinessLogicConflictException("Account is not a corporate account type.");
        }
        CorporateTransactionSearchDTO searchDTO = CorporateTransactionSearchDTO.builder()
                .corporate(mintAccountEntity)
                .approvalStatus(StringUtils.isEmpty(searchRequest.getApprovalStatus())? null : TransactionApprovalStatusConstant.valueOf(searchRequest.getApprovalStatus()))
                .fromDate(searchRequest.getFromDate())
                .toDate(searchRequest.getToDate())
                .transactionType(StringUtils.isEmpty(searchRequest.getTransactionType())? null : CorporateTransactionTypeConstant.valueOf(searchRequest.getTransactionType()))
                .build();
        Page<CorporateTransactionRequestEntity> entityPage = corporateTransactionRequestEntityDao.searchTransaction(searchDTO, page, size);

        return new PagedDataResponse<>(entityPage.getTotalElements(), entityPage.getTotalPages(),
                entityPage.get().map(this::fromEntityToModel).collect(Collectors.toList()));
    }

    private CorporateTransactionRequestModel fromEntityToModel(CorporateTransactionRequestEntity requestEntity) {
        AppUserEntity initiator = appUserEntityDao.getRecordById(requestEntity.getInitiator().getId());

        CorporateTransactionRequestModel requestModel = CorporateTransactionRequestModel.builder()
                .requestId(requestEntity.getRequestId())
                .initiatedBy(initiator.getName())
                .amount(requestEntity.getTotalAmount())
                .approvalStatus(requestEntity.getApprovalStatus().name())
                .dateRequested(requestEntity.getDateCreated().format(DateTimeFormatter.ISO_DATE_TIME))
                .transactionCategory(requestEntity.getTransactionCategory().name())
                .transactionType(requestEntity.getTransactionType().name())
                .transactionDescription(StringUtils.defaultString(requestEntity.getTransactionDescription()))
                .statusUpdatedReason(StringUtils.defaultString(requestEntity.getStatusUpdateReason()))
                .build();

        if(requestEntity.getReviewer() != null) {
            AppUserEntity reviewer = appUserEntityDao.getRecordById(requestEntity.getReviewer().getId());
            requestModel.setReviewedBy(reviewer.getName());
            requestModel.setDateReviewed(requestEntity.getDateReviewed().format(DateTimeFormatter.ISO_DATE_TIME));
        }
        return requestModel;
    }
}
