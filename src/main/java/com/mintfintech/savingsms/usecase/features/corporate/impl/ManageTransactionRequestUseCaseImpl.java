package com.mintfintech.savingsms.usecase.features.corporate.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.AccountAuthorisationUseCase;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.InvestmentCreationEvent;
import com.mintfintech.savingsms.usecase.data.request.CorporateApprovalRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.investment.CreateInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.FundInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.corporate.ManageTransactionRequestUseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

        accountAuthorisationUseCase.validationTransactionPin(transactionPin);



        if (!approved) {
            requestEntity.setApprovalStatus(TransactionApprovalStatusConstant.DECLINED);
            requestEntity.setStatusUpdateReason(request.getReason());
            requestEntity.setReviewer(user);
            requestEntity.setDateReviewed(LocalDateTime.now());
            transactionRequestEntityDao.saveRecord(requestEntity);

            CorporateTransactionEntity declinedTransaction = corporateTransactionEntityDao.getByTransactionRequest(requestEntity);
            if(declinedTransaction.getTransactionType().equals(CorporateTransactionTypeConstant.MUTUAL_INVESTMENT_TOPUP)){
                publishTransactionEvent(requestEntity);
                return "Investment top up declined.";
            }
            InvestmentEntity investmentEntity = investmentEntityDao.getRecordById(declinedTransaction.getTransactionRecordId());
            investmentEntity.setInvestmentStatus(InvestmentStatusConstant.CANCELLED);
            investmentEntityDao.saveRecord(investmentEntity);
            publishTransactionEvent(requestEntity);
            return "Investment declined successfully.";
        }

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

        if (transaction.getTransactionType().equals(CorporateTransactionTypeConstant.MUTUAL_INVESTMENT_TOPUP)) {
            if (transactionEntity.getTransactionStatus() != TransactionStatusConstant.SUCCESSFUL) {
                return "Sorry, account debit for investment funding failed.";
            }
            investmentEntity.setAmountInvested(investmentEntity.getAmountInvested().add(requestEntity.getTotalAmount()));
            investmentEntity.setTotalAmountInvested(investmentEntity.getTotalAmountInvested().add(requestEntity.getTotalAmount()));

        } else {
            if (transactionEntity.getTransactionStatus() != TransactionStatusConstant.SUCCESSFUL) {
                investmentEntity.setRecordStatus(RecordStatusConstant.DELETED);
                investmentEntityDao.saveRecord(investmentEntity);
                return "Sorry, account debit for investment funding failed.";
            }
            investmentEntity.setRecordStatus(RecordStatusConstant.ACTIVE);
            investmentEntity.setInvestmentStatus(InvestmentStatusConstant.ACTIVE);
            investmentEntity.setTotalAmountInvested(investAmount);

        }
        investmentEntityDao.saveRecord(investmentEntity);
        requestEntity.setApprovalStatus(TransactionApprovalStatusConstant.APPROVED);
        requestEntity.setReviewer(user);
        requestEntity.setDateReviewed(LocalDateTime.now());
        transactionRequestEntityDao.saveRecord(requestEntity);
        publishTransactionEvent(requestEntity);

        createInvestmentUseCase.sendInvestmentCreationEmail(investmentEntity, user);
        return "Approved successfully, details have been sent to your mail";
    }

    private void publishTransactionEvent(CorporateTransactionRequestEntity requestEntity) {
        InvestmentCreationEvent event = InvestmentCreationEvent.builder()
                .approvalStatus(requestEntity.getApprovalStatus().name())
                .dateReviewed(requestEntity.getDateReviewed())
                .userId(requestEntity.getReviewer().getUserId())
                .statusUpdateReason(requestEntity.getStatusUpdateReason())
                .build();

        EventModel<InvestmentCreationEvent> eventModel = new EventModel<>(event);
        applicationEventService.publishEvent(ApplicationEventService.EventType.CORPORATE_INVESTMENT_CREATION, eventModel);
    }
}
