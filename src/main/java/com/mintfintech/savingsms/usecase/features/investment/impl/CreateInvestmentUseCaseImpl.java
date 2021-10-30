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
import com.mintfintech.savingsms.usecase.data.events.outgoing.CorporateInvestmentCreationEmailEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.InvestmentCreationEmailEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.InvestmentCreationEvent;
import com.mintfintech.savingsms.usecase.data.request.CorporateApprovalRequest;
import com.mintfintech.savingsms.usecase.data.request.InvestmentCreationRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentCreationResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.UnauthorisedException;
import com.mintfintech.savingsms.usecase.features.investment.CreateInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.FundInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateInvestmentUseCaseImpl implements CreateInvestmentUseCase {

    private final AppUserEntityDao appUserEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final MintAccountEntityDao mintAccountEntityDao;
    private final UpdateBankAccountBalanceUseCase updateBankAccountBalanceUseCase;
    private final InvestmentTenorEntityDao investmentTenorEntityDao;
    private final InvestmentEntityDao investmentEntityDao;
    private final GetInvestmentUseCase getInvestmentUseCase;
    private final AccountAuthorisationUseCase accountAuthorisationUseCase;
    private final FundInvestmentUseCase fundInvestmentUseCase;
    private final ApplicationProperty applicationProperty;
    private final ApplicationEventService applicationEventService;
    private final CorporateUserEntityDao corporateUserEntityDao;
    private final CorporateTransactionRequestEntityDao transactionRequestEntityDao;
    private final CorporateTransactionEntityDao corporateTransactionEntityDao;

    @Override
    @Transactional
    public InvestmentCreationResponse createInvestment(AuthenticatedUser authenticatedUser, InvestmentCreationRequest request) {

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());

        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());

        accountAuthorisationUseCase.validationTransactionPin(request.getTransactionPin());
        InvestmentCreationResponse response;
        if (mintAccount.getAccountType().equals(AccountTypeConstant.INDIVIDUAL)) {
            response = processInvestment(appUser, mintAccount, request);
        } else if (mintAccount.getAccountType().equals(AccountTypeConstant.SOLE_PROPRIETORSHIP)) {
            response = processInvestment(appUser, mintAccount, request);
        } else if (mintAccount.getAccountType() != AccountTypeConstant.ENTERPRISE) {
            throw new BusinessLogicConflictException("Unrecognised account type.");
        } else {
            CorporateUserEntity corporateUser = corporateUserEntityDao.findRecordByAccountAndUser(mintAccount, appUser)
                    .orElseThrow(() -> new BusinessLogicConflictException("Sorry, user not found for corporate account"));
            CorporateRoleTypeConstant userRole = corporateUser.getRoleType();
            if (userRole == CorporateRoleTypeConstant.APPROVER) {
                throw new BusinessLogicConflictException("Sorry, you can only approve already initiated transaction");
            } else {
                response = createTransactionRequest(mintAccount, appUser, request);
            }
        }
        return response;
    }

    private InvestmentCreationResponse createTransactionRequest(MintAccountEntity mintAccount, AppUserEntity appUser, InvestmentCreationRequest request) {
        BigDecimal investAmount = BigDecimal.valueOf(request.getInvestmentAmount());

        InvestmentTenorEntity investmentTenor = investmentTenorEntityDao.findInvestmentTenorForDuration(request.getDurationInMonths(), RecordStatusConstant.ACTIVE)
                .orElseThrow(() -> new BadRequestException("Sorry, could not fetch a tenor for this duration"));

        InvestmentEntity investment = InvestmentEntity.builder()
                .amountInvested(investAmount)
                .code(investmentEntityDao.generateCode())
                .creator(appUser)
                .investmentStatus(InvestmentStatusConstant.INACTIVE)
                .investmentTenor(investmentTenor)
                .durationInMonths(request.getDurationInMonths())
                .maxLiquidateRate(applicationProperty.getMaxLiquidateRate())
                .owner(mintAccount)
                .totalAmountInvested(investAmount)
                .interestRate(investmentTenor.getInterestRate())
                .build();
        investment.setRecordStatus(RecordStatusConstant.INACTIVE);
        investment = investmentEntityDao.saveRecord(investment);

        CorporateTransactionRequestEntity transactionRequestEntity = CorporateTransactionRequestEntity.builder()
                .debitAccountId(request.getDebitAccountId())
                .transactionCategory(CorporateTransactionCategoryConstant.INVESTMENT)
                .transactionType(CorporateTransactionTypeConstant.MUTUAL_INVESTMENT)
                .approvalStatus(TransactionApprovalStatusConstant.PENDING)
                .corporate(mintAccount)
                .initiator(appUser)
                .totalAmount(investAmount)
                .transactionDescription("")
                .requestId(transactionRequestEntityDao.generateRequestId())
                .build();
        transactionRequestEntity = transactionRequestEntityDao.saveRecord(transactionRequestEntity);

        CorporateTransactionEntity transactionEntity = CorporateTransactionEntity.builder()
                .transactionRecordId(investment.getId())
                .transactionRequest(transactionRequestEntity)
                .corporate(mintAccount)
                .transactionType(CorporateTransactionTypeConstant.MUTUAL_INVESTMENT)
                .build();
        corporateTransactionEntityDao.saveRecord(transactionEntity);
        InvestmentCreationResponse response = new InvestmentCreationResponse();

        InvestmentCreationEvent event = InvestmentCreationEvent.builder()
                .approvalStatus(transactionRequestEntity.getApprovalStatus().name())
                .transactionCategory(transactionRequestEntity.getTransactionCategory().name())
                .debitAccountId(transactionRequestEntity.getDebitAccountId())
                .requestId(transactionRequestEntity.getRequestId())
                .totalAmount(transactionRequestEntity.getTotalAmount())
                .transactionType(transactionRequestEntity.getTransactionType().name())
                .transactionDescription(StringUtils.defaultString(transactionRequestEntity.getTransactionDescription()))
                .mintAccountId(transactionRequestEntity.getCorporate().getAccountId())
                .userId(transactionRequestEntity.getInitiator().getUserId())
                .build();

        EventModel<InvestmentCreationEvent> eventModel = new EventModel<>(event);
        applicationEventService.publishEvent(ApplicationEventService.EventType.CORPORATE_INVESTMENT_CREATION, eventModel);
        sendCorporateInvestmentCreationEmail(investment, transactionRequestEntity);
        response.setMessage("Your investment has been logged for approval. Details have been sent to your mail");
        return response;
    }

    private InvestmentCreationResponse processInvestment(AppUserEntity appUser, MintAccountEntity mintAccount, InvestmentCreationRequest request) {


        InvestmentTenorEntity investmentTenor = investmentTenorEntityDao.findInvestmentTenorForDuration(request.getDurationInMonths(), RecordStatusConstant.ACTIVE)
                .orElseThrow(() -> new BadRequestException("Sorry, could not fetch a tenor for this duration"));

        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(request.getDebitAccountId(), mintAccount)
                .orElseThrow(() -> new BadRequestException("Invalid debit account Id"));

        if (!mintAccount.getId().equals(debitAccount.getMintAccount().getId())) {
            throw new UnauthorisedException("Request denied.");
        }

        debitAccount = updateBankAccountBalanceUseCase.processBalanceUpdate(debitAccount);

        BigDecimal investAmount = BigDecimal.valueOf(request.getInvestmentAmount());

        if (debitAccount.getAvailableBalance().compareTo(investAmount) < 0) {
            InvestmentCreationResponse response = new InvestmentCreationResponse();
            response.setInvestment(null);
            response.setCreated(false);
            response.setMessage("Insufficient Funds");
            return response;
        }

        InvestmentEntity investment = InvestmentEntity.builder()
                .amountInvested(investAmount)
                .code(investmentEntityDao.generateCode())
                .creator(appUser)
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
        InvestmentTransactionEntity transactionEntity = fundInvestmentUseCase.fundInvestment(investment, debitAccount, investAmount);

        InvestmentCreationResponse response = new InvestmentCreationResponse();
        if (transactionEntity.getTransactionStatus() != TransactionStatusConstant.SUCCESSFUL) {
            investment.setRecordStatus(RecordStatusConstant.DELETED);
            investmentEntityDao.saveRecord(investment);
            response.setInvestment(null);
            response.setCreated(false);
            response.setMessage("Sorry, account debit for investment funding failed.");
            return response;
        }
        investment.setRecordStatus(RecordStatusConstant.ACTIVE);
        investment.setInvestmentStatus(InvestmentStatusConstant.ACTIVE);
        investment.setTotalAmountInvested(investAmount);
        investmentEntityDao.saveRecord(investment);

        sendInvestmentCreationEmail(investment, appUser);

        response.setInvestment(getInvestmentUseCase.toInvestmentModel(investment));
        response.setCreated(true);
        response.setMessage("Investment Created Successfully");
        return response;
    }

    @Override
    @Transactional
    public InvestmentCreationResponse createInvestmentByAdmin(AuthenticatedUser authenticatedUser, InvestmentCreationRequest request) {

        AppUserEntity owner = appUserEntityDao.findAppUserByUserId(request.getUserId()).orElseThrow(() -> new BadRequestException("Invalid user Id."));

        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(request.getDebitAccountId(), owner.getPrimaryAccount())
                .orElseThrow(() -> new BadRequestException("Invalid debit account Id"));

        InvestmentTenorEntity investmentTenor = investmentTenorEntityDao.findInvestmentTenorForDuration(request.getDurationInMonths(), RecordStatusConstant.ACTIVE)
                .orElseThrow(() -> new BadRequestException("Sorry, could not fetch a tenor for this duration"));

        debitAccount = updateBankAccountBalanceUseCase.processBalanceUpdate(debitAccount);

        BigDecimal investAmount = BigDecimal.valueOf(request.getInvestmentAmount());

        if (debitAccount.getAvailableBalance().compareTo(investAmount) < 0) {
            InvestmentCreationResponse response = new InvestmentCreationResponse();
            response.setInvestment(null);
            response.setCreated(false);
            response.setMessage("Insufficient Funds");
            return response;
        }

        InvestmentEntity investment = InvestmentEntity.builder()
                .amountInvested(investAmount)
                .code(investmentEntityDao.generateCode())
                .creator(owner)
                .investmentStatus(InvestmentStatusConstant.INACTIVE)
                .investmentTenor(investmentTenor)
                .durationInMonths(request.getDurationInMonths())
                .maturityDate(LocalDateTime.now().plusMonths(request.getDurationInMonths()))
                .maxLiquidateRate(applicationProperty.getMaxLiquidateRate())
                .owner(debitAccount.getMintAccount())
                .totalAmountInvested(investAmount)
                .interestRate(investmentTenor.getInterestRate())
                .managedByUser(authenticatedUser.getName())
                .managedByUserId(authenticatedUser.getUserId())
                .build();

        investment = investmentEntityDao.saveRecord(investment);

        InvestmentTransactionEntity transactionEntity = fundInvestmentUseCase.fundInvestment(investment, debitAccount, investAmount);

        InvestmentCreationResponse response = new InvestmentCreationResponse();
        if (transactionEntity.getTransactionStatus() != TransactionStatusConstant.SUCCESSFUL) {
            investment.setRecordStatus(RecordStatusConstant.DELETED);
            investmentEntityDao.saveRecord(investment);
            response.setInvestment(null);
            response.setCreated(false);
            response.setMessage("Sorry, account debit for investment funding failed.");
            return response;
        }
        investment.setRecordStatus(RecordStatusConstant.ACTIVE);
        investment.setInvestmentStatus(InvestmentStatusConstant.ACTIVE);
        investment.setTotalAmountInvested(investAmount);
        investmentEntityDao.saveRecord(investment);

        sendInvestmentCreationEmail(investment, owner);

        response.setInvestment(getInvestmentUseCase.toInvestmentModel(investment));
        response.setCreated(true);
        response.setMessage("Investment Created Successfully");
        return response;
    }

    private void sendInvestmentCreationEmail(InvestmentEntity investment, AppUserEntity appUser) {

        InvestmentCreationEmailEvent event = InvestmentCreationEmailEvent.builder()
                .duration(investment.getDurationInMonths())
                .investmentAmount(investment.getAmountInvested())
                .interestRate(investment.getInterestRate())
                .recipient(appUser.getEmail())
                .name(appUser.getName())
                .maturityDate(investment.getMaturityDate().format(DateTimeFormatter.ISO_DATE))
                .build();

        applicationEventService.publishEvent(ApplicationEventService.EventType.INVESTMENT_CREATION, new EventModel<>(event));


    }


    private void sendCorporateInvestmentCreationEmail(InvestmentEntity investment, CorporateTransactionRequestEntity requestEntity) {

        if (requestEntity.getApprovalStatus().equals(TransactionApprovalStatusConstant.PENDING)) {
            CorporateInvestmentCreationEmailEvent event = CorporateInvestmentCreationEmailEvent.builder()
                    .initiator(requestEntity.getInitiator().getName())
                    .initiatorEmail(requestEntity.getInitiator().getEmail())
                    .approvalStatus(requestEntity.getApprovalStatus().name())
                    .requestId(requestEntity.getRequestId())
                    .amount(requestEntity.getTotalAmount())
                    .duration(investment.getDurationInMonths())
                    .interestRate(investment.getInterestRate())
                    .maturityDate(StringUtils.defaultString(investment.getMaturityDate().format(DateTimeFormatter.ISO_DATE)))
                    .transactionType(requestEntity.getTransactionType().name())
                    .build();
            applicationEventService.publishEvent(ApplicationEventService.EventType.CORPORATE_INVESTMENT_CREATION, new EventModel<>(event));
            return;
        }
        CorporateInvestmentCreationEmailEvent event = CorporateInvestmentCreationEmailEvent.builder()
                .initiator(requestEntity.getInitiator().getName())
                .initiatorEmail(requestEntity.getInitiator().getEmail())
                .approvalStatus(requestEntity.getApprovalStatus().name())
                .requestId(requestEntity.getRequestId())
                .dateReviewed(requestEntity.getDateReviewed())
                .reviewer(requestEntity.getReviewer().getName())
                .reviewerEmail(requestEntity.getReviewer().getEmail())
                .statusUpdateReason(StringUtils.defaultString(requestEntity.getStatusUpdateReason()))
                .amount(requestEntity.getTotalAmount())
                .duration(investment.getDurationInMonths())
                .interestRate(investment.getInterestRate())
                .maturityDate(StringUtils.defaultString(investment.getMaturityDate().format(DateTimeFormatter.ISO_DATE)))
                .transactionType(requestEntity.getTransactionType().name())
                .build();

        applicationEventService.publishEvent(ApplicationEventService.EventType.CORPORATE_INVESTMENT_CREATION, new EventModel<>(event));
    }

    @Override
    public String approveCorporateInvestment(CorporateApprovalRequest request, AppUserEntity user, MintAccountEntity corporateAccount) {
        String requestId = request.getRequestId();
        boolean approved = request.isApproved();
        CorporateTransactionEntity transaction = corporateTransactionEntityDao.getByTransactionRequest(requestEntity);
        InvestmentEntity investmentEntity = investmentEntityDao.getRecordById(transaction.getTransactionRecordId());

        Optional<CorporateTransactionRequestEntity> requestEntityOptional = transactionRequestEntityDao.findByRequestId(requestId);
        if (!requestEntityOptional.isPresent()) {
            throw new BadRequestException("Invalid request Id.");
        }
        CorporateTransactionRequestEntity requestEntity = requestEntityOptional.get();
        if (!approved) {
            requestEntity.setApprovalStatus(TransactionApprovalStatusConstant.DECLINED);
            requestEntity.setStatusUpdateReason(StringUtils.defaultString(request.getReason()));
            requestEntity.setReviewer(user);
            requestEntity.setDateReviewed(LocalDateTime.now());
            transactionRequestEntityDao.saveRecord(requestEntity);

            investmentEntity.setInvestmentStatus(InvestmentStatusConstant.CANCELLED);
            investmentEntity.setRecordStatus(RecordStatusConstant.DELETED);
            investmentEntityDao.saveRecord(investmentEntity);

            publishTransactionEvent(requestEntity);
            sendCorporateInvestmentCreationEmail(investmentEntity, requestEntity);
            return "Investment declined successfully. Details have been sent to your mail";
        }
        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(requestEntity.getDebitAccountId(), corporateAccount)
                .orElseThrow(() -> new BusinessLogicConflictException("Debit account not found"));
        debitAccount = updateBankAccountBalanceUseCase.processBalanceUpdate(debitAccount);

        BigDecimal investAmount = requestEntity.getTotalAmount();
        if (debitAccount.getAvailableBalance().compareTo(investAmount) < 0) {
            throw new BadRequestException("Sorry, your account has insufficient balance");
        }

        int durationInMonths = investmentEntity.getDurationInMonths();
        InvestmentTransactionEntity transactionEntity = fundInvestmentUseCase.fundInvestment(investmentEntity, debitAccount, investAmount);

        if (transactionEntity.getTransactionStatus() != TransactionStatusConstant.SUCCESSFUL) {
            return "Sorry, account debit for investment funding failed. Please try again";
        }
        investmentEntity.setRecordStatus(RecordStatusConstant.ACTIVE);
        investmentEntity.setInvestmentStatus(InvestmentStatusConstant.ACTIVE);
        investmentEntity.setTotalAmountInvested(investAmount);
        investmentEntity.setMaturityDate(LocalDateTime.now().plusMonths(durationInMonths));
        investmentEntityDao.saveRecord(investmentEntity);

        requestEntity.setApprovalStatus(TransactionApprovalStatusConstant.APPROVED);
        requestEntity.setReviewer(user);
        requestEntity.setDateReviewed(LocalDateTime.now());
        transactionRequestEntityDao.saveRecord(requestEntity);

        publishTransactionEvent(requestEntity);
        sendCorporateInvestmentCreationEmail(investmentEntity, requestEntity);
        return "Approved successfully, details have been sent to your mail";
    }

    private void publishTransactionEvent(CorporateTransactionRequestEntity requestEntity) {
        InvestmentCreationEvent event = InvestmentCreationEvent.builder()
                .approvalStatus(requestEntity.getApprovalStatus().name())
                .dateReviewed(requestEntity.getDateReviewed())
                .userId(requestEntity.getReviewer().getUserId())
                .statusUpdateReason(StringUtils.defaultString(requestEntity.getStatusUpdateReason()))
                .build();

        EventModel<InvestmentCreationEvent> eventModel = new EventModel<>(event);
        applicationEventService.publishEvent(ApplicationEventService.EventType.CORPORATE_INVESTMENT_CREATION, eventModel);
    }

}
