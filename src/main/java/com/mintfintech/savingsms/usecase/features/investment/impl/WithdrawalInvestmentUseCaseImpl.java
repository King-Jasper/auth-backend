package com.mintfintech.savingsms.usecase.features.investment.impl;

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
import com.mintfintech.savingsms.usecase.data.events.outgoing.InvestmentCreationEmailEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.InvestmentLiquidationEmailEvent;
import com.mintfintech.savingsms.usecase.data.request.InvestmentWithdrawalRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.WithdrawalInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentModel;
import com.mintfintech.savingsms.utils.DateUtil;
import com.mintfintech.savingsms.utils.MoneyFormatterUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import javax.inject.Named;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

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


    @Override
    public InvestmentModel liquidateInvestment(AuthenticatedUser authenticatedUser, InvestmentWithdrawalRequest request) {

        InvestmentEntity investment = investmentEntityDao.findByCode(request.getInvestmentCode()).orElseThrow(() -> new BadRequestException("Invalid investment code."));

        if (investment.getInvestmentStatus() != InvestmentStatusConstant.ACTIVE) {
            throw new BadRequestException("Investment is not active. Current status - " + investment.getInvestmentStatus());
        }
        MintAccountEntity account = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());
        if (!account.getId().equals(investment.getOwner().getId())) {
            throw new BusinessLogicConflictException("Sorry, request cannot be processed.");
        }
        MintBankAccountEntity creditAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(request.getCreditAccountId(), account)
                .orElseThrow(() -> new BadRequestException("Invalid bank account Id."));

       // int minimumLiquidationPeriodInDays = 15;
        /*
        int minimumLiquidationPeriodInDays = applicationProperty.investmentMinimumLiquidationDays();
        if (!applicationProperty.isLiveEnvironment()) {
            minimumLiquidationPeriodInDays = 2;
        }

        long daysPast = investment.getDateCreated().until(LocalDateTime.now(), ChronoUnit.DAYS);
        if (daysPast < minimumLiquidationPeriodInDays) {
            throw new BusinessLogicConflictException("Sorry, your investment has to reach a minimum of " + minimumLiquidationPeriodInDays + " days before liquidation.");
        }
         */

        long hoursPast = investment.getDateCreated().until(LocalDateTime.now(), ChronoUnit.HOURS);
        if(hoursPast < 12) {
            throw new BusinessLogicConflictException("Sorry, your investment has to reach a minimum of 12 hours before liquidation.");
        }

        if (request.isFullLiquidation()) {
            processFullLiquidation(investment, creditAccount);
        } else {
            processPartialLiquidation(investment, creditAccount, request.getAmount());
        }
        return getInvestmentUseCase.toInvestmentModel(investment);
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
                .withdrawalStage(InvestmentWithdrawalStageConstant.PENDING_INTEREST_PENALTY_CHARGE)
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
        BigDecimal amountToWithdraw = amountInvested.add(accruedInterest.subtract(interestCharge));

        BigDecimal interestToWithdraw = accruedInterest.subtract(interestCharge);

        InvestmentWithdrawalEntity withdrawalEntity = InvestmentWithdrawalEntity.builder()
                .amount(amountToWithdraw)
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
                .totalAmount(amountToWithdraw)
                .build();
        investmentWithdrawalEntityDao.saveRecord(withdrawalEntity);

        investment.setAmountInvested(BigDecimal.ZERO);
        investment.setAccruedInterest(BigDecimal.ZERO);
        investment.setInvestmentStatus(InvestmentStatusConstant.LIQUIDATED);
        investment.setTotalInterestWithdrawn(investment.getTotalInterestWithdrawn().add(interestToWithdraw));
        investment.setTotalAmountWithdrawn(investment.getTotalAmountWithdrawn().add(amountToWithdraw));
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
        MintBankAccountEntity bankAccount = mintBankAccountEntityDao.getRecordById(withdrawal.getCreditAccount().getId());

        InvestmentTransactionEntity transaction = new InvestmentTransactionEntity();
        transaction.setInvestment(withdrawal.getInvestment());
        transaction.setBankAccount(withdrawal.getCreditAccount());
        transaction.setTransactionAmount(withdrawal.getInterest());
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

            if ("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                if (withdrawal.getWithdrawalType().equals(InvestmentWithdrawalTypeConstant.MATURITY_WITHDRAWAL)) {
                    withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.PENDING_TAX_PAYMENT);
                }

                if (withdrawal.getWithdrawalType().equals(InvestmentWithdrawalTypeConstant.FULL_PRE_MATURITY_WITHDRAWAL)) {
                    withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.PENDING_INTEREST_PENALTY_CHARGE);
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
            systemIssueLogService.logIssue("Investment Withdrawal Issue", "Liquidation penalty charge failed", message);
            withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.FAILED_PRE_LIQUIDATION_PENALTY);
            transaction.setTransactionStatus(TransactionStatusConstant.FAILED);
        } else {
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            transaction.setTransactionResponseMessage(responseCBS.getResponseMessage());
            transaction.setTransactionResponseCode(responseCBS.getResponseCode());

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
                systemIssueLogService.logIssue("Investment Withdrawal Issue", "Liquidation penalty charge failed", message);
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

        InvestmentTransactionEntity transaction = new InvestmentTransactionEntity();
        transaction.setInvestment(withdrawal.getInvestment());
        transaction.setBankAccount(withdrawal.getCreditAccount());
        transaction.setTransactionAmount(withdrawal.getInterestBeforeWithdrawal().multiply(BigDecimal.valueOf(0.1)));
        transaction.setTransactionReference(reference);
        transaction.setTransactionType(TransactionTypeConstant.DEBIT);
        transaction.setTransactionStatus(TransactionStatusConstant.PENDING);
        transaction.setFundingSource(FundingSourceTypeConstant.MINT_ACCOUNT);
        transaction.setTransactionDescription("Withholding Tax Charge.");
        transaction = investmentTransactionEntityDao.saveRecord(transaction);

        withdrawal.setWithholdingTaxCharge(transaction);
        withdrawal.setWithdrawalStage(InvestmentWithdrawalStageConstant.PROCESSING_TAX_PAYMENT);
        withdrawal = investmentWithdrawalEntityDao.saveRecord(withdrawal);

        InvestmentWithdrawalRequestCBS request = InvestmentWithdrawalRequestCBS.builder()
                .accountNumber(bankAccount.getAccountNumber())
                .narration(constructInvestmentNarration(investment.getCode(), reference))
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
