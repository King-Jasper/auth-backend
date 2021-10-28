package com.mintfintech.savingsms.usecase.features.investment.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.AccountAuthorisationUseCase;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.InvestmentCreationEvent;
import com.mintfintech.savingsms.usecase.data.request.CorporateApprovalRequest;
import com.mintfintech.savingsms.usecase.data.request.InvestmentCreationRequest;
import com.mintfintech.savingsms.usecase.data.response.CorporateInvestmentCreationResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.UnauthorisedException;
import com.mintfintech.savingsms.usecase.features.investment.CorporateInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.CreateInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.FundInvestmentUseCase;
import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@AllArgsConstructor
public class CorporateInvestmentUseCaseImpl implements CorporateInvestmentUseCase {

    private final AppUserEntityDao appUserEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final CorporateTransactionEntityDao corporateTransactionEntityDao;
    private final InvestmentTenorEntityDao investmentTenorEntityDao;
    private final InvestmentEntityDao investmentEntityDao;
    private final ApplicationProperty applicationProperty;
    private final CorporateUserEntityDao corporateUserEntityDao;
    private final CorporateTransactionRequestEntityDao transactionRequestEntityDao;
    private final AccountAuthorisationUseCase accountAuthorisationUseCase;
    private final UpdateBankAccountBalanceUseCase updateBankAccountBalanceUseCase;
    private final FundInvestmentUseCase fundInvestmentUseCase;
    private final CreateInvestmentUseCase createInvestmentUseCase;
    private final ApplicationEventService applicationEventService;


    @Override
    public CorporateInvestmentCreationResponse createInvestment(AuthenticatedUser authenticatedUser, InvestmentCreationRequest request) {
        AppUserEntity currentUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());

        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());

        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(request.getDebitAccountId(), mintAccount)
                .orElseThrow(() -> new BadRequestException("Invalid debit account Id"));

        InvestmentTenorEntity investmentTenor = investmentTenorEntityDao.findInvestmentTenorForDuration(request.getDurationInMonths(), RecordStatusConstant.ACTIVE)
                .orElseThrow(() -> new BadRequestException("Sorry, could not fetch a tenor for this duration"));

        if (!mintAccount.getId().equals(debitAccount.getMintAccount().getId())) {
            throw new UnauthorisedException("Request denied.");
        }

        if (mintAccount.getAccountType() != AccountTypeConstant.ENTERPRISE) {
            throw new BusinessLogicConflictException("Unrecognised account type.");
        } else {
            CorporateUserEntity corporateUser = corporateUserEntityDao.getRecordByAccountIdAndUserId(mintAccount, currentUser);

            CorporateRoleTypeConstant userRole = corporateUser.getRoleType();
            if (userRole == CorporateRoleTypeConstant.APPROVER) {
                throw new BusinessLogicConflictException("Sorry, you can only approve already initiated transaction");
            } else {
                BigDecimal investAmount = BigDecimal.valueOf(request.getInvestmentAmount());

                InvestmentEntity investment = InvestmentEntity.builder()
                        .amountInvested(investAmount)
                        .code(investmentEntityDao.generateCode())
                        .creator(currentUser)
                        .investmentStatus(InvestmentStatusConstant.INACTIVE)
                        .investmentTenor(investmentTenor)
                        .durationInMonths(request.getDurationInMonths())
                        .maturityDate(LocalDateTime.now().plusMonths(request.getDurationInMonths()))
                        .maxLiquidateRate(applicationProperty.getMaxLiquidateRate())
                        .owner(mintAccount)
                        .totalAmountInvested(investAmount)
                        .interestRate(investmentTenor.getInterestRate())
                        .build();

                investment = investmentEntityDao.saveRecord(investment);

                CorporateTransactionRequestEntity transactionRequestEntity = CorporateTransactionRequestEntity.builder()
                        .debitAccountId(request.getDebitAccountId())
                        .transactionCategory(CorporateTransactionCategoryConstant.INVESTMENT)
                        .transactionType(CorporateTransactionTypeConstant.INVESTMENT)
                        .approvalStatus(TransactionApprovalStatusConstant.PENDING)
                        .corporate(mintAccount)
                        .initiator(currentUser)
                        .totalAmount(investAmount)
                        .transactionDescription("")
                        .requestId(transactionRequestEntityDao.generateRequestId())
                        .build();
                transactionRequestEntity = transactionRequestEntityDao.saveRecord(transactionRequestEntity);

                CorporateTransactionEntity transactionEntity = CorporateTransactionEntity.builder()
                        .transactionRecordId(investment.getId())
                        .transactionRequest(transactionRequestEntity)
                        .corporate(mintAccount)
                        .transactionType(CorporateTransactionTypeConstant.INVESTMENT)
                        .build();
                corporateTransactionEntityDao.saveRecord(transactionEntity);

                InvestmentCreationEvent event = InvestmentCreationEvent.builder()
                        .approvalStatus(transactionRequestEntity.getApprovalStatus().name())
                        .transactionCategory(transactionRequestEntity.getTransactionCategory().name())
                        .debitAccountId(transactionRequestEntity.getDebitAccountId())
                        .requestId(transactionRequestEntity.getRequestId())
                        .totalAmount(transactionRequestEntity.getTotalAmount())
                        .transactionType(transactionRequestEntity.getTransactionType().name())
                        .transactionDescription(transactionRequestEntity.getTransactionDescription())
                        .mintAccountId(transactionRequestEntity.getCorporate().getAccountId())
                        .userId(transactionRequestEntity.getInitiator().getUserId())
                        .build();

                EventModel<InvestmentCreationEvent> eventModel = new EventModel<>(event);
                applicationEventService.publishEvent(ApplicationEventService.EventType.CORPORATE_INVESTMENT_CREATION, eventModel);

                return CorporateInvestmentCreationResponse.builder()
                        .InvestAmount(investAmount)
                        .responseCode("01")
                        .responseMessage("Your investment has been logged for approval.")
                        .transactionReference(investment.getCode())
                        .transactionDate(investment.getDateModified().format(DateTimeFormatter.ISO_DATE_TIME))
                        .build();
            }
        }
    }

    @Override
    public String processApproval(AuthenticatedUser currentUser, CorporateApprovalRequest request) {

        AppUserEntity user = appUserEntityDao.getAppUserByUserId("1234567");
        MintAccountEntity corporateAccount = mintAccountEntityDao.getAccountByAccountId("246890");

        String requestId = request.getRequestId();
        boolean approved = request.isApproved();
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

        if (!approved) {
            requestEntity.setApprovalStatus(TransactionApprovalStatusConstant.DECLINED);
            requestEntity.setStatusUpdateReason(request.getReason());
            requestEntity.setReviewer(user);
            requestEntity.setDateReviewed(LocalDateTime.now());
            transactionRequestEntityDao.saveRecord(requestEntity);

            CorporateTransactionEntity declinedTransaction = corporateTransactionEntityDao.getByTransactionRequest(requestEntity);

            InvestmentEntity investmentEntity = investmentEntityDao.getRecordById(declinedTransaction.getTransactionRecordId());
            investmentEntity.setInvestmentStatus(InvestmentStatusConstant.CANCELLED);
            investmentEntityDao.saveRecord(investmentEntity);

            return "Investment declined successfully.";
        }

        accountAuthorisationUseCase.validationTransactionPin(transactionPin);
        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(requestEntity.getDebitAccountId(), corporateAccount)
                .orElseThrow(() -> new BusinessLogicConflictException("Debit account not found"));
        debitAccount = updateBankAccountBalanceUseCase.processBalanceUpdate(debitAccount);

        BigDecimal investAmount = requestEntity.getTotalAmount();
        if (debitAccount.getAvailableBalance().compareTo(investAmount) < 0) {
            throw new BadRequestException("Sorry, your account has insufficient balance");
        }

        CorporateTransactionEntity transaction = corporateTransactionEntityDao.getByTransactionRequest(requestEntity);
        InvestmentEntity investmentEntity = investmentEntityDao.getRecordById(transaction.getTransactionRecordId());

        InvestmentTransactionEntity transactionEntity = fundInvestmentUseCase.fundInvestment(investmentEntity, debitAccount, investAmount);

        if (transactionEntity.getTransactionStatus() != TransactionStatusConstant.SUCCESSFUL) {
            investmentEntity.setRecordStatus(RecordStatusConstant.DELETED);
            investmentEntityDao.saveRecord(investmentEntity);
            return "Sorry, account debit for investment funding failed.";
        }
        investmentEntity.setRecordStatus(RecordStatusConstant.ACTIVE);
        investmentEntity.setInvestmentStatus(InvestmentStatusConstant.ACTIVE);
        investmentEntity.setTotalAmountInvested(investAmount);
        investmentEntityDao.saveRecord(investmentEntity);

        requestEntity.setApprovalStatus(TransactionApprovalStatusConstant.APPROVED);
        requestEntity.setReviewer(user);
        requestEntity.setDateReviewed(LocalDateTime.now());
        transactionRequestEntityDao.saveRecord(requestEntity);

        InvestmentCreationEvent event = InvestmentCreationEvent.builder()
                .approvalStatus(requestEntity.getApprovalStatus().name())
                .dateReviewed(requestEntity.getDateReviewed())
                .userId(requestEntity.getReviewer().getUserId())
                .statusUpdateReason(requestEntity.getStatusUpdateReason())
                .build();

        EventModel<InvestmentCreationEvent> eventModel = new EventModel<>(event);
        applicationEventService.publishEvent(ApplicationEventService.EventType.CORPORATE_INVESTMENT_CREATION, eventModel);

        createInvestmentUseCase.sendInvestmentCreationEmail(investmentEntity, user);

        return "Approved successfully, details have been sent to your mail";
    }

}