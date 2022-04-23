package com.mintfintech.savingsms.usecase.features.investment.impl;

import com.google.gson.Gson;
import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.corebankingservice.CBInvestmentWithdrawalStage;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.InvestmentWithdrawalRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.AccountAuthorisationUseCase;
import com.mintfintech.savingsms.usecase.PublishTransactionNotificationUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.CorporateInvestmentEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.CorporateInvestmentLiquidationEmailEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.InvestmentLiquidationEmailEvent;
import com.mintfintech.savingsms.usecase.data.request.CorporateApprovalRequest;
import com.mintfintech.savingsms.usecase.data.request.InvestmentWithdrawalRequest;
import com.mintfintech.savingsms.usecase.data.response.InvestmentLiquidationResponse;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.WithdrawalInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentDetailsInfo;
import com.mintfintech.savingsms.usecase.models.InvestmentLiquidationInfo;
import com.mintfintech.savingsms.utils.DateUtil;
import com.mintfintech.savingsms.utils.MoneyFormatterUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Thu, 20 May, 2021
 */
@Named
@Slf4j
@AllArgsConstructor
public class WithdrawalInvestmentUseCaseImpl implements WithdrawalInvestmentUseCase {

    private final MintAccountEntityDao mintAccountEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final InvestmentEntityDao investmentEntityDao;
    private final InvestmentTenorEntityDao investmentTenorEntityDao;
    private final InvestmentWithdrawalEntityDao investmentWithdrawalEntityDao;
    private final GetInvestmentUseCase getInvestmentUseCase;
    private final ApplicationProperty applicationProperty;
    private final InvestmentTransactionEntityDao investmentTransactionEntityDao;
    private final CoreBankingServiceClient coreBankingServiceClient;
    private final SystemIssueLogService systemIssueLogService;
    private final ApplicationEventService applicationEventService;
    private final AppUserEntityDao appUserEntityDao;
    private final CorporateUserEntityDao corporateUserEntityDao;
    private final CorporateTransactionRequestEntityDao transactionRequestEntityDao;
    private final CorporateTransactionEntityDao corporateTransactionEntityDao;
    private final AccountAuthorisationUseCase accountAuthorisationUseCase;
    private final PublishTransactionNotificationUseCase publishTransactionNotificationUseCase;
    private final Gson gson;


    @Override
    public InvestmentLiquidationResponse liquidateInvestment(AuthenticatedUser authenticatedUser, InvestmentWithdrawalRequest request) {

        InvestmentEntity investment = investmentEntityDao.findByCode(request.getInvestmentCode()).orElseThrow(() -> new BadRequestException("Invalid investment code."));

        if (investment.getInvestmentStatus() != InvestmentStatusConstant.ACTIVE) {
            throw new BadRequestException("Investment is not active. Current status - " + investment.getInvestmentStatus());
        }
        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());
        MintAccountEntity account = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        if (!account.getId().equals(investment.getOwner().getId())) {
            throw new BusinessLogicConflictException("Sorry, request cannot be processed.");
        }
        MintBankAccountEntity creditAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(request.getCreditAccountId(), account)
                .orElseThrow(() -> new BadRequestException("Invalid bank account Id."));

        // int minimumLiquidationPeriodInDays = 15;
        int minimumLiquidationPeriodInDays = applicationProperty.investmentMinimumLiquidationDays();
        if (!applicationProperty.isLiveEnvironment()) {
            minimumLiquidationPeriodInDays = 2;
        }

        long daysPast = investment.getDateCreated().until(LocalDateTime.now(), ChronoUnit.DAYS);
        if (daysPast < minimumLiquidationPeriodInDays) {
            throw new BusinessLogicConflictException("Sorry, your investment has to reach a minimum of " + minimumLiquidationPeriodInDays + " days before liquidation.");
        }

        InvestmentLiquidationResponse response = new InvestmentLiquidationResponse();
        String responseMessage = "Investment liquidated successfully.";
        if (account.getAccountType().equals(AccountTypeConstant.INDIVIDUAL)) {
            processLiquidation(request, investment, creditAccount);
            response.setMessage(responseMessage);
            response.setInvestmentModel(getInvestmentUseCase.toInvestmentModel(investment));
        } else if (account.getAccountType().equals(AccountTypeConstant.SOLE_PROPRIETORSHIP)) {
            if (StringUtils.isEmpty(request.getTransactionPin())) {
                throw new BadRequestException("Transaction pin is required");
            }
            accountAuthorisationUseCase.validationTransactionPin(request.getTransactionPin());
            processLiquidation(request, investment, creditAccount);
            response.setMessage(responseMessage);
            response.setInvestmentModel(getInvestmentUseCase.toInvestmentModel(investment));
        } else if (account.getAccountType() != AccountTypeConstant.ENTERPRISE) {
            throw new BusinessLogicConflictException("Unrecognised account type.");
        } else {
            if (StringUtils.isEmpty(request.getTransactionPin())) {
                throw new BadRequestException("Transaction pin is required");
            }
            accountAuthorisationUseCase.validationTransactionPin(request.getTransactionPin());

            CorporateUserEntity corporateUser = corporateUserEntityDao.findRecordByAccountAndUser(account, appUser)
                    .orElseThrow(() -> new BusinessLogicConflictException("Sorry, user not found for corporate account"));
            CorporateRoleTypeConstant userRole = corporateUser.getRoleType();
            if (userRole == CorporateRoleTypeConstant.APPROVER) {
                throw new BusinessLogicConflictException("Sorry, you can only approve already initiated transaction");
            }
            response = createTransactionRequest(account, investment, request, appUser);
            if (userRole == CorporateRoleTypeConstant.INITIATOR_AND_APPROVER) {
                CorporateApprovalRequest approvalRequest = CorporateApprovalRequest.builder()
                        .requestId(response.getRequestId())
                        .approved(true)
                        .build();
                approveInvestmentWithdrawal(approvalRequest, appUser, account);
                response.setMessage("Investment created and approved successfully.");
            }
        }
        return response;
    }

    private void processLiquidation(InvestmentWithdrawalRequest request, InvestmentEntity investment, MintBankAccountEntity creditAccount) {
        if (request.isFullLiquidation()) {
            processFullLiquidation(investment, creditAccount);
        } else {
            processPartialLiquidation(investment, creditAccount, request.getAmount());
        }
    }

    private InvestmentLiquidationResponse createTransactionRequest(MintAccountEntity account, InvestmentEntity investment, InvestmentWithdrawalRequest request, AppUserEntity appUser) {

        BigDecimal amountToWithdraw;
        boolean isFullLiquidation = false;
        if (request.isFullLiquidation()) {
            InvestmentTenorEntity tenorEntity = investmentTenorEntityDao.getRecordById(investment.getInvestmentTenor().getId());
            double interestPenaltyRate = tenorEntity.getPenaltyRate();
            BigDecimal amountInvested = investment.getAmountInvested();
            BigDecimal accruedInterest = investment.getAccruedInterest();
            BigDecimal interestCharge = BigDecimal.valueOf(accruedInterest.doubleValue() * (interestPenaltyRate / 100.0));
            amountToWithdraw = amountInvested.add(accruedInterest.subtract(interestCharge));

            isFullLiquidation = true;
        } else {
            amountToWithdraw = request.getAmount();
            double percentWithdrawal = 70;
            BigDecimal amountInvest = investment.getAmountInvested();
            BigDecimal maximumWithdrawalAmount = amountInvest.subtract(BigDecimal.valueOf(amountInvest.doubleValue() * (percentWithdrawal / 100.0)));

            if (amountToWithdraw.compareTo(maximumWithdrawalAmount) > 0) {
                String errorMessage = String.format("Maximum amount you can withdraw is %s. That is %s percent of investment amount.",
                        MoneyFormatterUtil.priceWithDecimal(maximumWithdrawalAmount), MoneyFormatterUtil.priceWithoutDecimal(percentWithdrawal));
                throw new BusinessLogicConflictException(errorMessage);
            }
        }
        InvestmentLiquidationInfo liquidationInfo = InvestmentLiquidationInfo.builder()
                .fullLiquidation(isFullLiquidation)
                .amountToWithdraw(amountToWithdraw)
                .build();
        String transactionMetaData = gson.toJson(liquidationInfo, InvestmentLiquidationInfo.class);

        CorporateTransactionRequestEntity transactionRequestEntity = CorporateTransactionRequestEntity.builder()
                .debitAccountId(request.getCreditAccountId())
                .transactionCategory(CorporateTransactionCategoryConstant.INVESTMENT)
                .transactionType(CorporateTransactionTypeConstant.MUTUAL_INVESTMENT_LIQUIDATION)
                .approvalStatus(TransactionApprovalStatusConstant.PENDING)
                .corporate(account)
                .initiator(appUser)
                .totalAmount(amountToWithdraw)
                .transactionDescription("")
                .requestId(transactionRequestEntityDao.generateRequestId())
                .transactionMetaData(transactionMetaData)
                .build();
        transactionRequestEntity = transactionRequestEntityDao.saveRecord(transactionRequestEntity);

        CorporateTransactionEntity transactionEntity = CorporateTransactionEntity.builder()
                .transactionRecordId(investment.getId())
                .transactionRequest(transactionRequestEntity)
                .corporate(account)
                .transactionType(CorporateTransactionTypeConstant.MUTUAL_INVESTMENT_LIQUIDATION)
                .build();
        corporateTransactionEntityDao.saveRecord(transactionEntity);

        CorporateInvestmentEvent event = CorporateInvestmentEvent.builder()
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

        EventModel<CorporateInvestmentEvent> eventModel = new EventModel<>(event);
        applicationEventService.publishEvent(ApplicationEventService.EventType.CORPORATE_INVESTMENT_REQUEST, eventModel);
        sendCorporateEmailNotification(investment, transactionRequestEntity);
        Optional<CorporateUserEntity> opt = corporateUserEntityDao.findRecordByAccountAndUser(account, appUser);
        CorporateUserEntity corporateUser = opt.get();
        CorporateRoleTypeConstant userRole = corporateUser.getRoleType();
        if (userRole.equals(CorporateRoleTypeConstant.INITIATOR)) {
            publishTransactionNotificationUseCase.sendPendingCorporateInvestmentNotification(account);
        }
        InvestmentLiquidationResponse response = new InvestmentLiquidationResponse();
        response.setRequestId(transactionRequestEntity.getRequestId());
        response.setMessage("Investment liquidation logged for approval.");
        return response;
    }

    private void sendCorporateEmailNotification(InvestmentEntity investment, CorporateTransactionRequestEntity transactionRequestEntity) {

        InvestmentTenorEntity tenorEntity = investmentTenorEntityDao.getRecordById(investment.getInvestmentTenor().getId());
        double interestPenaltyRate = tenorEntity.getPenaltyRate();

        CorporateInvestmentLiquidationEmailEvent emailEvent = CorporateInvestmentLiquidationEmailEvent.builder()
                .recipient(transactionRequestEntity.getInitiator().getEmail())
                .name(transactionRequestEntity.getInitiator().getName())
                .investmentAmount(investment.getAmountInvested())
                .maturityDate(investment.getMaturityDate().format(DateTimeFormatter.ISO_DATE))
                .penaltyRate(interestPenaltyRate)
                .build();
        String metaData = transactionRequestEntity.getTransactionMetaData();
        BigDecimal amountInvested = investment.getAmountInvested();
        BigDecimal accruedInterest = investment.getAccruedInterest();

        BigDecimal interestCharge = BigDecimal.valueOf(accruedInterest.doubleValue() * (interestPenaltyRate / 100.0));
        BigDecimal totalWithdrawalAmount = amountInvested.add(accruedInterest.subtract(interestCharge));
        InvestmentLiquidationInfo liquidationInfo = gson.fromJson(metaData, InvestmentLiquidationInfo.class);
        if (liquidationInfo.isFullLiquidation()) {
            emailEvent.setInvestmentBalance(BigDecimal.ZERO);
            emailEvent.setLiquidatedAmount(totalWithdrawalAmount);

        } else {
            emailEvent.setLiquidatedAmount(transactionRequestEntity.getTotalAmount());
            emailEvent.setInvestmentBalance(investment.getAmountInvested().subtract(transactionRequestEntity.getTotalAmount()));
        }

        EventModel<CorporateInvestmentLiquidationEmailEvent> emailEventModel = new EventModel<>(emailEvent);
        applicationEventService.publishEvent(ApplicationEventService.EventType.CORPORATE_INVESTMENT_LIQUIDATION, emailEventModel);
    }

    @Override
    public void processInterestPayout() {
        List<InvestmentWithdrawalEntity> withdrawals = investmentWithdrawalEntityDao.getWithdrawalByInvestmentAndStatus(InvestmentWithdrawalStageConstant.PENDING_INTEREST_TO_CUSTOMER);

        if (!withdrawals.isEmpty()) {
            log.info("Withdrawal request pending interest payout: {}", withdrawals.size());
        }

        for (InvestmentWithdrawalEntity withdrawal : withdrawals) {
            processInterestPayoutPayment(withdrawal);
        }
    }

    @Override
    public void processPenaltyChargePayout() {
        List<InvestmentWithdrawalEntity> withdrawals = investmentWithdrawalEntityDao.getWithdrawalByInvestmentAndStatus(InvestmentWithdrawalStageConstant.PENDING_INTEREST_PENALTY_CHARGE);

        if (!withdrawals.isEmpty()) {
            log.info("Withdrawal request pending interest penalty charge: {}", withdrawals.size());
        }

        for (InvestmentWithdrawalEntity withdrawal : withdrawals) {
            processInterestPenaltyChargePayment(withdrawal);
        }
    }

    @Override
    public void processWithholdingTaxPayout() {
        List<InvestmentWithdrawalEntity> withdrawals = investmentWithdrawalEntityDao.getWithdrawalByInvestmentAndStatus(InvestmentWithdrawalStageConstant.PENDING_TAX_PAYMENT);

        if (!withdrawals.isEmpty()) {
            log.info("Withdrawal request pending tax payment: {}", withdrawals.size());
        }

        for (InvestmentWithdrawalEntity withdrawal : withdrawals) {
            processTaxPayment(withdrawal);
        }
    }

    @Override
    public void processPrincipalPayout() {
        List<InvestmentWithdrawalEntity> withdrawals = investmentWithdrawalEntityDao.getWithdrawalByInvestmentAndStatus(InvestmentWithdrawalStageConstant.PENDING_PRINCIPAL_TO_CUSTOMER);

        if (!withdrawals.isEmpty()) {
            log.info("Withdrawal request principal payout: {}", withdrawals.size());
        }

        for (InvestmentWithdrawalEntity withdrawal : withdrawals) {
            processPrincipalPayment(withdrawal);
        }
    }

    @Override
    public String approveInvestmentWithdrawal(CorporateApprovalRequest request, AppUserEntity user, MintAccountEntity corporateAccount) {

        String requestId = request.getRequestId();
        boolean approved = request.isApproved();

        Optional<CorporateTransactionRequestEntity> requestEntityOptional = transactionRequestEntityDao.findByRequestId(requestId);
        if (!requestEntityOptional.isPresent()) {
            throw new BadRequestException("Invalid request Id.");
        }
        CorporateTransactionRequestEntity requestEntity = requestEntityOptional.get();
        CorporateTransactionEntity transaction = corporateTransactionEntityDao.getByTransactionRequest(requestEntity);
        InvestmentEntity investmentEntity = investmentEntityDao.getRecordById(transaction.getTransactionRecordId());

        BigDecimal amountInvested = investmentEntity.getAmountInvested();
        BigDecimal expectedReturns;
        String transactionMetaData;

        InvestmentDetailsInfo investmentDetailsInfo = InvestmentDetailsInfo.builder()
                .amountInvested(amountInvested)
                .interestRate(investmentEntity.getInterestRate())
                .maturityDate(investmentEntity.getMaturityDate().format(DateTimeFormatter.ISO_DATE_TIME))
                .interestAccrued(investmentEntity.getAccruedInterest())
                .totalExpectedReturns(getInvestmentUseCase.calculateTotalExpectedReturn(investmentEntity.getAmountInvested(), investmentEntity.getAccruedInterest(), investmentEntity.getInterestRate(), investmentEntity.getMaturityDate()))
                .build();
        transactionMetaData = gson.toJson(investmentDetailsInfo, InvestmentDetailsInfo.class);
        transaction.setTransactionMetaData(transactionMetaData);
        corporateTransactionEntityDao.saveRecord(transaction);

        if (!approved) {

            requestEntity.setApprovalStatus(TransactionApprovalStatusConstant.DECLINED);
            requestEntity.setStatusUpdateReason(StringUtils.defaultString(request.getReason()));
            requestEntity.setReviewer(user);
            requestEntity.setDateReviewed(LocalDateTime.now());
            transactionRequestEntityDao.saveRecord(requestEntity);
            publishTransactionEvent(requestEntity);
            publishTransactionNotificationUseCase.sendDeclinedCorporateInvestmentNotification(corporateAccount);
            return "Investment withdrawal declined successfully.";
        }
        MintBankAccountEntity creditAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(requestEntity.getDebitAccountId(), corporateAccount)
                .orElseThrow(() -> new BadRequestException("Invalid bank account Id."));

        String metaData = requestEntity.getTransactionMetaData();
        InvestmentLiquidationInfo liquidationInfo = gson.fromJson(metaData, InvestmentLiquidationInfo.class);

        boolean isFullLiquidation = liquidationInfo.isFullLiquidation();
        if (isFullLiquidation) {
            processFullLiquidation(investmentEntity, creditAccount);
            expectedReturns = BigDecimal.ZERO;
        } else {
            processPartialLiquidation(investmentEntity, creditAccount, requestEntity.getTotalAmount());
            expectedReturns = getInvestmentUseCase.calculateTotalExpectedReturn(investmentEntity.getAmountInvested(), investmentEntity.getAccruedInterest(),
                    investmentEntity.getInterestRate(), investmentEntity.getMaturityDate());
        }

        investmentDetailsInfo.setInterestAccrued(investmentEntity.getAccruedInterest());
        investmentDetailsInfo.setTotalExpectedReturns(expectedReturns);
        transactionMetaData = gson.toJson(investmentDetailsInfo, InvestmentDetailsInfo.class);

        requestEntity.setTransactionMetaData(transactionMetaData);
        requestEntity.setApprovalStatus(TransactionApprovalStatusConstant.APPROVED);
        requestEntity.setReviewer(user);
        requestEntity.setDateReviewed(LocalDateTime.now());
        transactionRequestEntityDao.saveRecord(requestEntity);

        publishTransactionEvent(requestEntity);
        return "Approved successfully, details have been sent to the initiator";
    }

    private void publishTransactionEvent(CorporateTransactionRequestEntity requestEntity) {
        MintAccountEntity mintAccountEntity = requestEntity.getCorporate();
        if(!Hibernate.isInitialized(mintAccountEntity)) {
            mintAccountEntity = mintAccountEntityDao.getRecordById(mintAccountEntity.getId());
        }
        CorporateInvestmentEvent event = CorporateInvestmentEvent.builder()
                .approvalStatus(requestEntity.getApprovalStatus().name())
                .dateReviewed(requestEntity.getDateReviewed())
                .userId(requestEntity.getReviewer().getUserId())
                .mintAccountId(mintAccountEntity.getAccountId())
                .requestId(requestEntity.getRequestId())
                .statusUpdateReason(StringUtils.defaultString(requestEntity.getStatusUpdateReason()))
                .build();

        EventModel<CorporateInvestmentEvent> eventModel = new EventModel<>(event);
        applicationEventService.publishEvent(ApplicationEventService.EventType.CORPORATE_INVESTMENT_REQUEST, eventModel);

    }

    private void processPartialLiquidation(InvestmentEntity investment, MintBankAccountEntity creditAccount, BigDecimal amountToWithdraw) {
        double percentWithdrawal = 70;
        BigDecimal amountInvest = investment.getAmountInvested();
        BigDecimal maximumWithdrawalAmount = amountInvest.subtract(BigDecimal.valueOf(amountInvest.doubleValue() * (percentWithdrawal / 100.0)));

        if (amountToWithdraw.compareTo(maximumWithdrawalAmount) > 0) {
            String errorMessage = String.format("Maximum amount you can withdraw is %s. That is %s percent of investment amount.",
                    MoneyFormatterUtil.priceWithDecimal(maximumWithdrawalAmount), MoneyFormatterUtil.priceWithoutDecimal(percentWithdrawal));
            throw new BusinessLogicConflictException(errorMessage);
        }

        InvestmentTenorEntity tenorEntity = investmentTenorEntityDao.getRecordById(investment.getInvestmentTenor().getId());
        double interestPenaltyRate = tenorEntity.getPenaltyRate();

        BigDecimal accruedInterest = investment.getAccruedInterest();

        BigDecimal interestCharge = BigDecimal.valueOf(accruedInterest.doubleValue() * (interestPenaltyRate / 100.0));

        InvestmentWithdrawalStageConstant withdrawalStage;
        if(interestCharge.compareTo(BigDecimal.ZERO) == 0) {
            withdrawalStage = InvestmentWithdrawalStageConstant.PENDING_TAX_PAYMENT;
        }else {
            withdrawalStage = InvestmentWithdrawalStageConstant.PENDING_INTEREST_PENALTY_CHARGE;
        }
        InvestmentWithdrawalEntity withdrawalEntity = InvestmentWithdrawalEntity.builder()
                .amount(amountToWithdraw)
                .amountCharged(interestCharge)
                .balanceBeforeWithdrawal(amountInvest)
                .interestBeforeWithdrawal(accruedInterest)
                .dateForWithdrawal(DateUtil.addWorkingDays(LocalDate.now(), 2))
                .interest(BigDecimal.ZERO)
                .investment(investment)
                .matured(false)
                .creditAccount(creditAccount)
                .withdrawalStage(withdrawalStage)
                .withdrawalType(InvestmentWithdrawalTypeConstant.PART_PRE_MATURITY_WITHDRAWAL)
                .requestedBy(investment.getCreator())
                .totalAmount(amountToWithdraw)
                .build();
        investmentWithdrawalEntityDao.saveRecord(withdrawalEntity);

        investment.setAmountInvested(amountInvest.subtract(amountToWithdraw));
        investment.setAccruedInterest(accruedInterest.subtract(interestCharge));
        investment.setTotalAmountWithdrawn(investment.getTotalAmountWithdrawn().add(amountToWithdraw));
        investmentEntityDao.saveRecord(investment);

        sendInvestmentLiquidationEmail(investment, withdrawalEntity);
    }

    private void processFullLiquidation(InvestmentEntity investment, MintBankAccountEntity creditAccount) {

        InvestmentTenorEntity tenorEntity = investmentTenorEntityDao.getRecordById(investment.getInvestmentTenor().getId());
        double interestPenaltyRate = tenorEntity.getPenaltyRate();
        BigDecimal amountInvested = investment.getAmountInvested();
        BigDecimal accruedInterest = investment.getAccruedInterest();

        BigDecimal interestCharge = BigDecimal.valueOf(accruedInterest.doubleValue() * (interestPenaltyRate / 100.0));
        BigDecimal principalToWithdraw = amountInvested;
        BigDecimal totalWithdrawalAmount = principalToWithdraw.add(accruedInterest.subtract(interestCharge));

        BigDecimal interestToWithdraw = accruedInterest.subtract(interestCharge);

        InvestmentWithdrawalEntity withdrawalEntity = InvestmentWithdrawalEntity.builder()
                .amount(principalToWithdraw)
                .amountCharged(interestCharge)
                .balanceBeforeWithdrawal(amountInvested)
                .interestBeforeWithdrawal(accruedInterest)
                .dateForWithdrawal(DateUtil.addWorkingDays(LocalDate.now(), 2))
                .interest(interestToWithdraw)
                .investment(investment)
                .matured(false)
                .creditAccount(creditAccount)
                .withdrawalStage(InvestmentWithdrawalStageConstant.PENDING_INTEREST_TO_CUSTOMER)
                .withdrawalType(InvestmentWithdrawalTypeConstant.FULL_PRE_MATURITY_WITHDRAWAL)
                .requestedBy(investment.getCreator())
                .totalAmount(totalWithdrawalAmount)
                .build();
        investmentWithdrawalEntityDao.saveRecord(withdrawalEntity);

        investment.setAmountInvested(BigDecimal.ZERO);
        investment.setAccruedInterest(BigDecimal.ZERO);
        investment.setInvestmentStatus(InvestmentStatusConstant.LIQUIDATED);
        investment.setTotalInterestWithdrawn(investment.getTotalInterestWithdrawn().add(interestToWithdraw));
        investment.setTotalAmountWithdrawn(investment.getTotalAmountWithdrawn().add(totalWithdrawalAmount));
        investment.setDateWithdrawn(LocalDateTime.now());
        investmentEntityDao.saveRecord(investment);

        sendInvestmentLiquidationEmail(investment, withdrawalEntity);
    }

    private void sendInvestmentLiquidationEmail(InvestmentEntity investment, InvestmentWithdrawalEntity withdrawal) {

        AppUserEntity appUser = appUserEntityDao.getRecordById(investment.getCreator().getId());

        InvestmentLiquidationEmailEvent event = InvestmentLiquidationEmailEvent.builder()
                .investmentAmount(withdrawal.getBalanceBeforeWithdrawal())
                .investmentBalance(investment.getAmountInvested())
                .liquidatedAmount(withdrawal.getAmount())
                .maturityDate(investment.getMaturityDate().format(DateTimeFormatter.ISO_DATE))
                .name(appUser.getName())
                .penaltyCharge(withdrawal.getAmountCharged())
                .recipient(appUser.getEmail())
                .build();

        applicationEventService.publishEvent(ApplicationEventService.EventType.INVESTMENT_LIQUIDATION_SUCCESS, new EventModel<>(event));
    }

    private void processInterestPayoutPayment(InvestmentWithdrawalEntity withdrawal) {
        String reference = investmentTransactionEntityDao.generateTransactionReference();
        InvestmentEntity investment = investmentEntityDao.getRecordById(withdrawal.getInvestment().getId());
        InvestmentTenorEntity investmentTenor = investment.getInvestmentTenor();
        MintBankAccountEntity bankAccount = mintBankAccountEntityDao.getRecordById(withdrawal.getCreditAccount().getId());

        BigDecimal interestAmount = withdrawal.getInterest();
        if (!withdrawal.isMatured()) {
            interestAmount = withdrawal.getInterestBeforeWithdrawal(); //penalty charge will be on this.
        }

        InvestmentTransactionEntity transaction = new InvestmentTransactionEntity();
        transaction.setInvestment(withdrawal.getInvestment());
        transaction.setBankAccount(withdrawal.getCreditAccount());
        transaction.setTransactionAmount(interestAmount);
        transaction.setTransactionReference(reference);
        transaction.setTransactionType(TransactionTypeConstant.DEBIT);
        transaction.setTransactionStatus(TransactionStatusConstant.PENDING);
        transaction.setFundingSource(FundingSourceTypeConstant.MINT_ACCOUNT);
        transaction.setTransactionDescription("Interest payout.");
        transaction = investmentTransactionEntityDao.saveRecord(transaction);

        withdrawal.setInterestPayout(transaction);
        withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.PROCESSING_INTEREST_TO_CUSTOMER);
        withdrawal = investmentWithdrawalEntityDao.saveRecord(withdrawal);

        String narration = "Investment(" + investment.getCode() + ") interest payment - " + reference;

        InvestmentWithdrawalRequestCBS request = InvestmentWithdrawalRequestCBS.builder()
                .accountNumber(bankAccount.getAccountNumber())
                .narration(narration)
                .transactionAmount(transaction.getTransactionAmount().doubleValue())
                .transactionReference(reference)
                .withdrawalStage(CBInvestmentWithdrawalStage.INTEREST_PAYOUT.name())
                .withdrawalType(withdrawal.getWithdrawalType().name())
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processInvestmentWithdrawal(request);

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value() || msClientResponse.getData() == null) {
            String message = String.format("Investment Id: %s; transaction Id: %s ; message: %s", investment.getCode(), reference, msClientResponse.getMessage());
            systemIssueLogService.logIssue("Investment Withdrawal Issue", "Interest payout failed", message);
            withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.FAILED_INTEREST_TO_CUSTOMER);
            transaction.setTransactionStatus(TransactionStatusConstant.FAILED);
        } else {
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            transaction.setTransactionResponseMessage(responseCBS.getResponseMessage());
            transaction.setTransactionResponseCode(responseCBS.getResponseCode());
            transaction.setExternalReference(responseCBS.getBankOneReference());

            if ("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                if (withdrawal.getWithdrawalType().equals(InvestmentWithdrawalTypeConstant.MATURITY_WITHDRAWAL)) {
                    withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.PENDING_TAX_PAYMENT);
                }
                if (withdrawal.getWithdrawalType().equals(InvestmentWithdrawalTypeConstant.FULL_PRE_MATURITY_WITHDRAWAL)) {
                    if(withdrawal.getAmountCharged().compareTo(BigDecimal.ZERO) == 0) {
                        withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.PENDING_TAX_PAYMENT);
                    }else {
                        withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.PENDING_INTEREST_PENALTY_CHARGE);
                    }
                }
                transaction.setTransactionStatus(TransactionStatusConstant.SUCCESSFUL);
            } else {
                String message = String.format("Investment Id: %s; transaction Id: %s ; message: %s", investment.getCode(), reference, msClientResponse.getMessage());
                systemIssueLogService.logIssue("Investment Withdrawal Issue", "Interest payout failed", message);
                withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.FAILED_INTEREST_TO_CUSTOMER);
                transaction.setTransactionStatus(TransactionStatusConstant.FAILED);
            }
        }
        investmentTransactionEntityDao.saveRecord(transaction);
        investmentWithdrawalEntityDao.saveRecord(withdrawal);
    }

    private void processInterestPenaltyChargePayment(InvestmentWithdrawalEntity withdrawal) {
        String reference = investmentTransactionEntityDao.generateTransactionReference();
        InvestmentEntity investment = investmentEntityDao.getRecordById(withdrawal.getInvestment().getId());
        MintBankAccountEntity bankAccount = mintBankAccountEntityDao.getRecordById(withdrawal.getCreditAccount().getId());

        InvestmentTransactionEntity transaction = new InvestmentTransactionEntity();
        transaction.setInvestment(withdrawal.getInvestment());
        transaction.setBankAccount(withdrawal.getCreditAccount());
        transaction.setTransactionAmount(withdrawal.getAmountCharged());
        transaction.setTransactionReference(reference);
        transaction.setTransactionType(TransactionTypeConstant.DEBIT);
        transaction.setTransactionStatus(TransactionStatusConstant.PENDING);
        transaction.setFundingSource(FundingSourceTypeConstant.MINT_ACCOUNT);
        transaction.setTransactionDescription("Liquidation penalty charge.");
        transaction = investmentTransactionEntityDao.saveRecord(transaction);

        withdrawal.setPenaltyCharge(transaction);
        withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.PROCESSING_PRE_LIQUIDATION_PENALTY);
        withdrawal = investmentWithdrawalEntityDao.saveRecord(withdrawal);

        String narration = "Investment(" + investment.getCode() + ") liquidation penalty charge - " + reference;

        InvestmentWithdrawalRequestCBS request = InvestmentWithdrawalRequestCBS.builder()
                .accountNumber(bankAccount.getAccountNumber())
                .narration(narration)
                .transactionAmount(transaction.getTransactionAmount().doubleValue())
                .transactionReference(reference)
                .withdrawalStage(CBInvestmentWithdrawalStage.INTEREST_PENALTY_CHARGE.name())
                .withdrawalType(withdrawal.getWithdrawalType().name())
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processInvestmentWithdrawal(request);

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value() || msClientResponse.getData() == null) {
            String message = String.format("Investment Id: %s; transaction Id: %s ; message: %s", investment.getCode(), reference, msClientResponse.getMessage());
            systemIssueLogService.logIssue("Liquidation penalty charge failed", message, request.toString());
            withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.FAILED_PRE_LIQUIDATION_PENALTY);
            transaction.setTransactionStatus(TransactionStatusConstant.FAILED);
        } else {
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            transaction.setTransactionResponseMessage(responseCBS.getResponseMessage());
            transaction.setTransactionResponseCode(responseCBS.getResponseCode());
            transaction.setExternalReference(responseCBS.getBankOneReference());

            if ("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                if (withdrawal.getWithdrawalType().equals(InvestmentWithdrawalTypeConstant.PART_PRE_MATURITY_WITHDRAWAL)) {
                    withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.PENDING_PRINCIPAL_TO_CUSTOMER);
                }

                if (withdrawal.getWithdrawalType().equals(InvestmentWithdrawalTypeConstant.FULL_PRE_MATURITY_WITHDRAWAL)) {
                    withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.PENDING_TAX_PAYMENT);
                }
                transaction.setTransactionStatus(TransactionStatusConstant.SUCCESSFUL);

            } else {
                String message = String.format("Investment Id: %s; transaction Id: %s ; message: %s", investment.getCode(), reference, msClientResponse.getMessage());
                systemIssueLogService.logIssue("Liquidation penalty charge failed", message, request.toString());
                withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.FAILED_PRE_LIQUIDATION_PENALTY);
                transaction.setTransactionStatus(TransactionStatusConstant.FAILED);
            }
        }
        investmentTransactionEntityDao.saveRecord(transaction);
        investmentWithdrawalEntityDao.saveRecord(withdrawal);
    }

    private void processTaxPayment(InvestmentWithdrawalEntity withdrawal) {
        String reference = investmentTransactionEntityDao.generateTransactionReference();
        InvestmentEntity investment = investmentEntityDao.getRecordById(withdrawal.getInvestment().getId());
        MintBankAccountEntity bankAccount = mintBankAccountEntityDao.getRecordById(withdrawal.getCreditAccount().getId());

        BigDecimal taxAmount = withdrawal.getInterestBeforeWithdrawal().multiply(BigDecimal.valueOf(0.1));

        InvestmentTransactionEntity transaction = new InvestmentTransactionEntity();
        transaction.setInvestment(withdrawal.getInvestment());
        transaction.setBankAccount(withdrawal.getCreditAccount());
        transaction.setTransactionAmount(taxAmount);
        transaction.setTransactionReference(reference);
        transaction.setTransactionType(TransactionTypeConstant.DEBIT);
        transaction.setTransactionStatus(TransactionStatusConstant.PENDING);
        transaction.setFundingSource(FundingSourceTypeConstant.MINT_ACCOUNT);
        transaction.setTransactionDescription("Withholding Tax Charge.");
        transaction = investmentTransactionEntityDao.saveRecord(transaction);

        withdrawal.setWithholdingTaxCharge(transaction);
        withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.PROCESSING_TAX_PAYMENT);
        withdrawal = investmentWithdrawalEntityDao.saveRecord(withdrawal);

        String narration = "Investment(" + investment.getCode() + ") withholding Tax Charge - " + reference;

        InvestmentWithdrawalRequestCBS request = InvestmentWithdrawalRequestCBS.builder()
                .accountNumber(bankAccount.getAccountNumber())
                .narration(narration)
                .transactionAmount(transaction.getTransactionAmount().doubleValue())
                .transactionReference(reference)
                .withdrawalStage(CBInvestmentWithdrawalStage.TAX_PAYMENT.name())
                .withdrawalType(withdrawal.getWithdrawalType().name())
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processInvestmentWithdrawal(request);

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value() || msClientResponse.getData() == null) {
            String message = String.format("Investment Id: %s; transaction Id: %s ; message: %s", investment.getCode(), reference, msClientResponse.getMessage());
            systemIssueLogService.logIssue("Investment Withdrawal Issue", "Withholding tax payment failed", message);
            withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.FAILED_TAX_PAYMENT);
            transaction.setTransactionStatus(TransactionStatusConstant.FAILED);
        } else {
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            transaction.setTransactionResponseMessage(responseCBS.getResponseMessage());
            transaction.setTransactionResponseCode(responseCBS.getResponseCode());
            transaction.setExternalReference(responseCBS.getBankOneReference());

            if ("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                if (withdrawal.getWithdrawalType().equals(InvestmentWithdrawalTypeConstant.MATURITY_WITHDRAWAL)) {
                    withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.PENDING_PRINCIPAL_TO_CUSTOMER);
                }

                if (withdrawal.getWithdrawalType().equals(InvestmentWithdrawalTypeConstant.FULL_PRE_MATURITY_WITHDRAWAL)) {
                    withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.PENDING_PRINCIPAL_TO_CUSTOMER);
                }
                transaction.setTransactionStatus(TransactionStatusConstant.SUCCESSFUL);

            } else {
                String message = String.format("Investment Id: %s; transaction Id: %s ; message: %s", investment.getCode(), reference, msClientResponse.getMessage());
                systemIssueLogService.logIssue("Investment Withdrawal Issue", "Withholding tax payment failed", message);
                withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.FAILED_TAX_PAYMENT);
                transaction.setTransactionStatus(TransactionStatusConstant.FAILED);
            }
        }
        investmentTransactionEntityDao.saveRecord(transaction);
        investmentWithdrawalEntityDao.saveRecord(withdrawal);
    }

    private void processPrincipalPayment(InvestmentWithdrawalEntity withdrawal) {
        String reference = investmentTransactionEntityDao.generateTransactionReference();
        InvestmentEntity investment = investmentEntityDao.getRecordById(withdrawal.getInvestment().getId());
        MintBankAccountEntity bankAccount = mintBankAccountEntityDao.getRecordById(withdrawal.getCreditAccount().getId());

        InvestmentTransactionEntity transaction = new InvestmentTransactionEntity();
        transaction.setInvestment(withdrawal.getInvestment());
        transaction.setBankAccount(withdrawal.getCreditAccount());
        transaction.setTransactionAmount(withdrawal.getAmount());
        transaction.setTransactionReference(reference);
        transaction.setTransactionType(TransactionTypeConstant.DEBIT);
        transaction.setTransactionStatus(TransactionStatusConstant.PENDING);
        transaction.setFundingSource(FundingSourceTypeConstant.MINT_ACCOUNT);
        transaction.setTransactionDescription("Investment principal payout.");
        transaction = investmentTransactionEntityDao.saveRecord(transaction);

        withdrawal.setPrincipalPayout(transaction);
        withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.PROCESSING_PRINCIPAL_TO_CUSTOMER);
        withdrawal = investmentWithdrawalEntityDao.saveRecord(withdrawal);

        InvestmentWithdrawalRequestCBS request = InvestmentWithdrawalRequestCBS.builder()
                .accountNumber(bankAccount.getAccountNumber())
                .narration(constructInvestmentNarration(investment.getCode(), reference))
                .transactionAmount(transaction.getTransactionAmount().doubleValue())
                .transactionReference(reference)
                .withdrawalStage(CBInvestmentWithdrawalStage.PRINCIPAL_PAYOUT.name())
                .withdrawalType(withdrawal.getWithdrawalType().name())
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processInvestmentWithdrawal(request);

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value() || msClientResponse.getData() == null) {
            String message = String.format("Investment Id: %s; transaction Id: %s ; message: %s", investment.getCode(), reference, msClientResponse.getMessage());
            systemIssueLogService.logIssue("Investment Withdrawal Issue", "Principal payout failed", message);
            withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.FAILED_PRINCIPAL_TO_CUSTOMER);
            transaction.setTransactionStatus(TransactionStatusConstant.FAILED);
        } else {
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            transaction.setTransactionResponseMessage(responseCBS.getResponseMessage());
            transaction.setTransactionResponseCode(responseCBS.getResponseCode());
            transaction.setExternalReference(responseCBS.getBankOneReference());

            if ("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.COMPLETED);
                transaction.setTransactionStatus(TransactionStatusConstant.SUCCESSFUL);
            } else {
                String message = String.format("Investment Id: %s; transaction Id: %s ; message: %s", investment.getCode(), reference, msClientResponse.getMessage());
                systemIssueLogService.logIssue("Investment Withdrawal Issue", "Principal payout failed", message);
                withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.FAILED_PRINCIPAL_TO_CUSTOMER);
                transaction.setTransactionStatus(TransactionStatusConstant.FAILED);
            }
        }
        investmentTransactionEntityDao.saveRecord(transaction);
        investmentWithdrawalEntityDao.saveRecord(withdrawal);
    }


    private String constructInvestmentNarration(String investmentCode, String reference) {
        String narration = String.format("Mint Investment withdrawal %s %s", investmentCode, reference);
        if (narration.length() > 61) {
            return narration.substring(0, 60);
        }
        return narration;
    }
}
