package com.mintfintech.savingsms.usecase.features.investment.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.InvestmentFundingRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.AccountAuthorisationUseCase;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.InvestmentCreationEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.InvestmentFundingEmailEvent;
import com.mintfintech.savingsms.usecase.data.request.CorporateApprovalRequest;
import com.mintfintech.savingsms.usecase.data.request.InvestmentFundingRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentFundingResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.investment.FundInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by jnwanya on
 * Wed, 19 May, 2021
 */
@Named
@AllArgsConstructor
public class FundInvestmentUseCaseImpl implements FundInvestmentUseCase {

    private final InvestmentTransactionEntityDao investmentTransactionEntityDao;
    private final InvestmentEntityDao investmentEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final UpdateBankAccountBalanceUseCase updateBankAccountBalanceUseCase;
    private final GetInvestmentUseCase getInvestmentUseCase;
    private final CoreBankingServiceClient coreBankingServiceClient;
    private final SystemIssueLogService systemIssueLogService;
    private final ApplicationEventService applicationEventService;
    private final AppUserEntityDao appUserEntityDao;
    private final AccountAuthorisationUseCase accountAuthorisationUseCase;
    private final CorporateUserEntityDao corporateUserEntityDao;
    private final CorporateTransactionRequestEntityDao transactionRequestEntityDao;
    private final CorporateTransactionEntityDao corporateTransactionEntityDao;

    @Override
    public InvestmentTransactionEntity fundInvestment(InvestmentEntity investmentEntity, MintBankAccountEntity debitAccount, BigDecimal amount) {

        String reference = investmentTransactionEntityDao.generateTransactionReference();

        InvestmentTransactionEntity transaction = new InvestmentTransactionEntity();
        transaction.setInvestment(investmentEntity);
        transaction.setBankAccount(debitAccount);
        transaction.setTransactionAmount(amount);
        transaction.setTransactionReference(reference);
        transaction.setTransactionType(TransactionTypeConstant.CREDIT);
        transaction.setTransactionStatus(TransactionStatusConstant.PENDING);
        transaction.setFundingSource(FundingSourceTypeConstant.MINT_ACCOUNT);
        transaction.setTransactionDescription("Investment funding");
        transaction = investmentTransactionEntityDao.saveRecord(transaction);

        processDebit(transaction, investmentEntity, debitAccount.getAccountNumber());

        return investmentTransactionEntityDao.saveRecord(transaction);
    }

    @Override
    public InvestmentFundingResponse fundInvestment(AuthenticatedUser authenticatedUser, InvestmentFundingRequest request) {
        InvestmentEntity investmentEntity = investmentEntityDao.findByCode(request.getInvestmentCode()).orElseThrow(() -> new BadRequestException("Invalid investment code."));
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());
        if (!investmentEntity.getOwner().getId().equals(accountEntity.getId())) {
            throw new BusinessLogicConflictException("Sorry, request cannot be processed.");
        }
        BigDecimal amount = request.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("Invalid amount.");
        }
        if (investmentEntity.getInvestmentStatus() != InvestmentStatusConstant.ACTIVE) {
            throw new BusinessLogicConflictException("Sorry, investment is no longer active. Current status - " + investmentEntity.getInvestmentStatus());
        }
        LocalDateTime maturityDate = investmentEntity.getMaturityDate();
        if (LocalDateTime.now().compareTo(maturityDate) >= 0) {
            throw new BusinessLogicConflictException("Sorry, your investment has already matured.");
        }

        InvestmentFundingResponse response;
        if (accountEntity.getAccountType().equals(AccountTypeConstant.INDIVIDUAL)) {
            response = processInvestmentTopUp(request, investmentEntity, accountEntity);
        } else if (accountEntity.getAccountType().equals(AccountTypeConstant.SOLE_PROPRIETORSHIP)) {
            if (StringUtils.isEmpty(request.getTransactionPin())) {
                throw new BadRequestException("Transaction pin is required");
            }
            accountAuthorisationUseCase.validationTransactionPin(request.getTransactionPin());
            response = processInvestmentTopUp(request, investmentEntity, accountEntity);
        } else if (accountEntity.getAccountType() != AccountTypeConstant.ENTERPRISE) {
            throw new BusinessLogicConflictException("Unrecognised account type.");
        } else {

            if (StringUtils.isEmpty(request.getTransactionPin())) {
                throw new BadRequestException("Transaction pin is required");
            }
            accountAuthorisationUseCase.validationTransactionPin(request.getTransactionPin());
            CorporateUserEntity corporateUser = corporateUserEntityDao.findRecordByAccountAndUser(accountEntity, appUser)
                    .orElseThrow(() -> new BusinessLogicConflictException("Sorry, user not found for corporate account"));
            CorporateRoleTypeConstant userRole = corporateUser.getRoleType();
            if (userRole == CorporateRoleTypeConstant.APPROVER) {
                throw new BusinessLogicConflictException("Sorry, you can only approve already initiated transaction");
            } else {
                response = createTransactionRequest(accountEntity, investmentEntity, request, appUser);
            }
        }

        return response;
    }

    private InvestmentFundingResponse createTransactionRequest(MintAccountEntity mintAccount, InvestmentEntity investmentEntity, InvestmentFundingRequest request, AppUserEntity appUser) {

        CorporateTransactionRequestEntity transactionRequestEntity = CorporateTransactionRequestEntity.builder()
                .requestId(transactionRequestEntityDao.generateRequestId())
                .transactionCategory(CorporateTransactionCategoryConstant.INVESTMENT)
                .transactionType(CorporateTransactionTypeConstant.MUTUAL_INVESTMENT_TOPUP)
                .approvalStatus(TransactionApprovalStatusConstant.PENDING)
                .corporate(mintAccount)
                .initiator(appUser)
                .debitAccountId(request.getDebitAccountId())
                .totalAmount(request.getAmount())
                .build();
        transactionRequestEntity = transactionRequestEntityDao.saveRecord(transactionRequestEntity);

        CorporateTransactionEntity transactionEntity = CorporateTransactionEntity.builder()
                .transactionRecordId(investmentEntity.getId())
                .transactionRequest(transactionRequestEntity)
                .corporate(mintAccount)
                .transactionType(CorporateTransactionTypeConstant.MUTUAL_INVESTMENT_TOPUP)
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

        InvestmentFundingResponse response = new InvestmentFundingResponse();
        EventModel<InvestmentCreationEvent> eventModel = new EventModel<>(event);
        applicationEventService.publishEvent(ApplicationEventService.EventType.CORPORATE_INVESTMENT_CREATION, eventModel);
        response.setResponseCode("01");
        response.setResponseMessage("Investment top-Up has been logged for approval");
        return response;
    }

    private InvestmentFundingResponse processInvestmentTopUp(InvestmentFundingRequest request, InvestmentEntity investmentEntity, MintAccountEntity accountEntity) {

        BigDecimal amount = request.getAmount();
        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(request.getDebitAccountId(), accountEntity)
                .orElseThrow(() -> new BadRequestException("Invalid debit account."));

        debitAccount = updateBankAccountBalanceUseCase.processBalanceUpdate(debitAccount);
        if (debitAccount.getAvailableBalance().compareTo(amount) < 0) {
            throw new BusinessLogicConflictException("Sorry, you have insufficient balance to fund your investment.");
        }
        InvestmentTransactionEntity transactionEntity = fundInvestment(investmentEntity, debitAccount, amount);
        InvestmentFundingResponse response = new InvestmentFundingResponse();
        String responseCode = "00";
        if (transactionEntity.getTransactionStatus() != TransactionStatusConstant.SUCCESSFUL) {
            responseCode = transactionEntity.getTransactionStatus() == TransactionStatusConstant.PENDING ? "01" : "02";
            response.setResponseCode(responseCode);
            response.setInvestment(getInvestmentUseCase.toInvestmentModel(investmentEntity));
            return response;
        }
        investmentEntity.setAmountInvested(investmentEntity.getAmountInvested().add(amount));
        investmentEntity.setTotalAmountInvested(investmentEntity.getTotalAmountInvested().add(amount));
        investmentEntityDao.saveRecord(investmentEntity);

        sendInvestmentFundingSuccessEmail(investmentEntity, transactionEntity.getTransactionAmount());

        response.setResponseCode(responseCode);
        response.setInvestment(getInvestmentUseCase.toInvestmentModel(investmentEntity));
        return response;
    }

    @Override
    public InvestmentFundingResponse fundInvestmentByAdmin(InvestmentFundingRequest request) {
        InvestmentEntity investmentEntity = investmentEntityDao.findByCode(request.getInvestmentCode()).orElseThrow(() -> new BadRequestException("Invalid investment code."));
        MintAccountEntity accountEntity = mintAccountEntityDao.getAccountByAccountId(request.getAccountId());

        if (!investmentEntity.getOwner().getId().equals(accountEntity.getId())) {
            throw new BusinessLogicConflictException("Sorry, request cannot be processed.");
        }
        BigDecimal amount = request.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("Invalid amount.");
        }
        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(request.getDebitAccountId(), accountEntity)
                .orElseThrow(() -> new BadRequestException("Invalid debit account."));
        debitAccount = updateBankAccountBalanceUseCase.processBalanceUpdate(debitAccount);

        if (debitAccount.getAvailableBalance().compareTo(amount) < 0) {
            throw new BusinessLogicConflictException("Sorry, you have insufficient balance to fund your investment.");
        }
        if (investmentEntity.getInvestmentStatus() != InvestmentStatusConstant.ACTIVE) {
            throw new BusinessLogicConflictException("Sorry is no longer active. Current status - " + investmentEntity.getInvestmentStatus());
        }
        LocalDateTime maturityDate = investmentEntity.getMaturityDate();
        if (LocalDateTime.now().compareTo(maturityDate) >= 0) {
            throw new BusinessLogicConflictException("Sorry, your investment has already matured.");
        }


        InvestmentTransactionEntity transactionEntity = fundInvestment(investmentEntity, debitAccount, amount);
        InvestmentFundingResponse response = new InvestmentFundingResponse();
        String responseCode = "00";
        if (transactionEntity.getTransactionStatus() != TransactionStatusConstant.SUCCESSFUL) {
            responseCode = transactionEntity.getTransactionStatus() == TransactionStatusConstant.PENDING ? "01" : "02";
            response.setResponseCode(responseCode);
            response.setInvestment(getInvestmentUseCase.toInvestmentModel(investmentEntity));
            return response;
        }
        investmentEntity.setAmountInvested(investmentEntity.getAmountInvested().add(amount));
        investmentEntity.setTotalAmountInvested(investmentEntity.getTotalAmountInvested().add(amount));
        investmentEntityDao.saveRecord(investmentEntity);

        sendInvestmentFundingSuccessEmail(investmentEntity, transactionEntity.getTransactionAmount());

        response.setResponseCode(responseCode);
        response.setInvestment(getInvestmentUseCase.toInvestmentModel(investmentEntity));
        return response;
    }

    @Override
    public String approveCorporateInvestmentTopUp(CorporateTransactionRequestEntity requestEntity, CorporateApprovalRequest request, AppUserEntity user, MintAccountEntity corporateAccount) {

        boolean approved = request.isApproved();
        if (!approved) {
            requestEntity.setApprovalStatus(TransactionApprovalStatusConstant.DECLINED);
            requestEntity.setStatusUpdateReason(request.getReason());
            requestEntity.setReviewer(user);
            requestEntity.setDateReviewed(LocalDateTime.now());
            transactionRequestEntityDao.saveRecord(requestEntity);
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

        InvestmentTransactionEntity transactionEntity = fundInvestment(investmentEntity, debitAccount, investAmount);
        if (transactionEntity.getTransactionStatus() != TransactionStatusConstant.SUCCESSFUL) {
            return "Sorry, account debit for investment funding failed. Please try again";
        }
        investmentEntity.setAmountInvested(investmentEntity.getAmountInvested().add(requestEntity.getTotalAmount()));
        investmentEntity.setTotalAmountInvested(investmentEntity.getTotalAmountInvested().add(requestEntity.getTotalAmount()));
        investmentEntityDao.saveRecord(investmentEntity);
        requestEntity.setApprovalStatus(TransactionApprovalStatusConstant.APPROVED);
        requestEntity.setReviewer(user);
        requestEntity.setDateReviewed(LocalDateTime.now());
        transactionRequestEntityDao.saveRecord(requestEntity);

        publishTransactionEvent(requestEntity);
        sendInvestmentFundingSuccessEmail(investmentEntity, requestEntity.getTotalAmount());
        return "Approved successfully, details have been sent to your mail";
    }

    private void processDebit(InvestmentTransactionEntity transaction, InvestmentEntity investment, String accountNumber) {

        String narration = constructInvestmentNarration(investment.getCode(), transaction.getTransactionReference());

        InvestmentFundingRequestCBS request = InvestmentFundingRequestCBS.builder()
                .transactionAmount(transaction.getTransactionAmount().doubleValue())
                .accountNumber(accountNumber)
                .transactionReference(transaction.getTransactionReference())
                .narration(narration)
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processInvestmentFunding(request);

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value() || msClientResponse.getData() == null) {
            String message = String.format("Investment Id: %s; transaction ref: %s ; message: %s", investment.getCode(), transaction.getTransactionReference(), msClientResponse.getMessage());
            systemIssueLogService.logIssue("Investment Funding Issue", "Customer investment funding failed", message);
            transaction.setTransactionStatus(TransactionStatusConstant.FAILED);
        } else {
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            transaction.setTransactionResponseCode(responseCBS.getResponseCode());
            transaction.setTransactionResponseMessage(responseCBS.getResponseMessage());
            transaction.setExternalReference(responseCBS.getBankOneReference());

            if ("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                transaction.setTransactionStatus(TransactionStatusConstant.SUCCESSFUL);
            } else {
                transaction.setTransactionStatus(TransactionStatusConstant.FAILED);
                String message = String.format("Investment Id: %s; transaction ref: %s ; message: %s", investment.getCode(), transaction.getTransactionReference(), msClientResponse.getMessage());
                systemIssueLogService.logIssue("Investment Funding Issue", "Customer investment funding failed", message);
            }

        }
    }

    private String constructInvestmentNarration(String investmentCode, String reference) {
        String narration = String.format("Mint Investment funding %s %s", investmentCode, reference);
        if (narration.length() > 61) {
            return narration.substring(0, 60);
        }
        return narration;
    }

    private void sendInvestmentFundingSuccessEmail(InvestmentEntity investment, BigDecimal amount) {
        AppUserEntity appUser = appUserEntityDao.getRecordById(investment.getCreator().getId());
        InvestmentFundingEmailEvent event = InvestmentFundingEmailEvent.builder()
                .amount(amount)
                .investmentBalance(investment.getAmountInvested())
                .duration(investment.getDurationInMonths())
                .interestRate(investment.getInterestRate())
                .maturityDate(investment.getMaturityDate().format(DateTimeFormatter.ISO_DATE))
                .recipient(appUser.getEmail())
                .name(appUser.getName())
                .build();

        applicationEventService.publishEvent(ApplicationEventService.EventType.INVESTMENT_FUNDING_SUCCESS, new EventModel<>(event));
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
