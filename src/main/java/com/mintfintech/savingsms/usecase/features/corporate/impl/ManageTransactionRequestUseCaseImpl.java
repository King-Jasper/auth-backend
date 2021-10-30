package com.mintfintech.savingsms.usecase.features.corporate.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.CorporateTransactionRequestEntity;
import com.mintfintech.savingsms.domain.entities.CorporateUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.CorporateRoleTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.CorporateTransactionCategoryConstant;
import com.mintfintech.savingsms.domain.entities.enums.CorporateTransactionTypeConstant;
import com.mintfintech.savingsms.domain.entities.enums.TransactionApprovalStatusConstant;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.AccountAuthorisationUseCase;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.request.CorporateApprovalRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.corporate.ManageTransactionRequestUseCase;
import com.mintfintech.savingsms.usecase.features.investment.CreateInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.FundInvestmentUseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class ManageTransactionRequestUseCaseImpl implements ManageTransactionRequestUseCase {

    private final AppUserEntityDao appUserEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final CorporateTransactionEntityDao corporateTransactionEntityDao;
    private final InvestmentEntityDao investmentEntityDao;
    private final CorporateUserEntityDao corporateUserEntityDao;
    private final CorporateTransactionRequestEntityDao transactionRequestEntityDao;
    private final AccountAuthorisationUseCase accountAuthorisationUseCase;
    private final UpdateBankAccountBalanceUseCase updateBankAccountBalanceUseCase;
    private final FundInvestmentUseCase fundInvestmentUseCase;
    private final CreateInvestmentUseCase createInvestmentUseCase;
    private final ApplicationEventService applicationEventService;


    @Override
    public String processApproval(AuthenticatedUser currentUser, CorporateApprovalRequest request) {

        AppUserEntity user = appUserEntityDao.getAppUserByUserId(currentUser.getUserId());
        MintAccountEntity corporateAccount = mintAccountEntityDao.getAccountByAccountId(currentUser.getAccountId());

        String requestId = request.getRequestId();
        String transactionPin = request.getTransactionPin();

        Optional<CorporateTransactionRequestEntity> requestEntityOptional = transactionRequestEntityDao.findByRequestId(requestId);
        if (!requestEntityOptional.isPresent()) {
            throw new BadRequestException("Invalid request Id.");
        }
        CorporateTransactionRequestEntity requestEntity = requestEntityOptional.get();

        if (requestEntity.getApprovalStatus() != TransactionApprovalStatusConstant.PENDING) {
            throw new BusinessLogicConflictException("Sorry, investment request is not on PENDING APPROVAL state.");
        }

        if (requestEntity.getTransactionCategory() != CorporateTransactionCategoryConstant.INVESTMENT) {
            throw new BusinessLogicConflictException("Sorry, request cannot be processed by this service. " + requestEntity.getTransactionCategory());
        }

        Optional<CorporateUserEntity> corporateUserEntityOptional = corporateUserEntityDao.findRecordByAccountAndUser(corporateAccount, user);

        if (!corporateUserEntityOptional.isPresent()) {
            throw new BusinessLogicConflictException("Corporate user record not found.");
        }

        CorporateUserEntity corporateUser = corporateUserEntityOptional.get();
        CorporateRoleTypeConstant roleConstant = corporateUser.getRoleType();
        if (roleConstant != CorporateRoleTypeConstant.APPROVER && roleConstant != CorporateRoleTypeConstant.INITIATOR_AND_APPROVER) {
            throw new BusinessLogicConflictException("Sorry, you do not have an APPROVER role.");
        }

        accountAuthorisationUseCase.validationTransactionPin(transactionPin);

        String response = "";
        if (requestEntity.getTransactionType().equals(CorporateTransactionTypeConstant.MUTUAL_INVESTMENT)) {
            response = createInvestmentUseCase.approveCorporateInvestment(request, user, corporateAccount);
        } else if (requestEntity.getTransactionType().equals(CorporateTransactionTypeConstant.MUTUAL_INVESTMENT_TOPUP)) {
            response = fundInvestmentUseCase.approveCorporateInvestmentTopUp(request, user, corporateAccount);
        }

        return response;
    }
}