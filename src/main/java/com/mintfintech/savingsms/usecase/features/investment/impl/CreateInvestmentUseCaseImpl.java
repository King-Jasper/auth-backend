package com.mintfintech.savingsms.usecase.features.investment.impl;

import com.mintfintech.savingsms.domain.dao.*;
import com.mintfintech.savingsms.domain.entities.*;
import com.mintfintech.savingsms.domain.entities.enums.*;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.MintFundTransferRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.AccountAuthorisationUseCase;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.request.InvestmentCreationRequest;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.UnauthorisedException;
import com.mintfintech.savingsms.usecase.features.investment.CreateInvestmentUseCase;
import com.mintfintech.savingsms.usecase.features.investment.GetInvestmentUseCase;
import com.mintfintech.savingsms.usecase.models.InvestmentCreationResponseModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private final InvestmentTransactionEntityDao investmentTransactionEntityDao;
    private final CoreBankingServiceClient coreBankingServiceClient;
    private final SystemIssueLogService systemIssueLogService;

    @Override
    @Transactional
    public InvestmentCreationResponseModel createInvestment(AuthenticatedUser authenticatedUser, InvestmentCreationRequest request) {

        AppUserEntity appUser = appUserEntityDao.getAppUserByUserId(authenticatedUser.getUserId());

        MintAccountEntity mintAccount = mintAccountEntityDao.getAccountByAccountId(authenticatedUser.getAccountId());

        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.findByAccountIdAndMintAccount(request.getDebitAccountId(), mintAccount)
                .orElseThrow(() -> new BadRequestException("Invalid debit account Id"));

        InvestmentTenorEntity investmentTenor = investmentTenorEntityDao.findById(Long.getLong(request.getTenorId()))
                .orElseThrow(() -> new BadRequestException("Invalid investment tenor Id"));

        if (!mintAccount.getId().equals(debitAccount.getMintAccount().getId())) {
            throw new UnauthorisedException("Request denied.");
        }

        accountAuthorisationUseCase.validationTransactionPin(request.getTransactionPin());

        debitAccount = updateBankAccountBalanceUseCase.processBalanceUpdate(debitAccount);

        BigDecimal investAmount = BigDecimal.valueOf(request.getInvestmentAmount());

        if (debitAccount.getAvailableBalance().compareTo(investAmount) < 0) {
            InvestmentCreationResponseModel response = new InvestmentCreationResponseModel();
            response.setInvestment(null);
            response.setCreated(false);
            response.setMessage("Insufficient Funds");

            return response;
        }

        int durationInDays = (investmentTenor.getMaximumDuration() + 1) - investmentTenor.getMinimumDuration();

        InvestmentEntity investment = InvestmentEntity.builder()
                .amountInvested(investAmount)
                .code(investmentEntityDao.generateCode())
                .creator(appUser)
                .investmentStatus(SavingsGoalStatusConstant.ACTIVE)
                .investmentTenor(investmentTenor)
                .durationInDays(durationInDays)
                .lastInterestApplicationDate(LocalDateTime.now().plusDays(durationInDays - 1))
                .maturityDate(LocalDateTime.now().plusDays(durationInDays))
                .owner(mintAccount)
                .build();

        investment = investmentEntityDao.saveRecord(investment);

        boolean isCustomerDebited = debitCustomerAccount(investment, debitAccount);

        InvestmentCreationResponseModel response = new InvestmentCreationResponseModel();

        if (isCustomerDebited) {
            response.setInvestment(getInvestmentUseCase.toInvestmentModel(investment));
            response.setCreated(true);
            response.setMessage("Investment Created Successfully");
        } else {
            investment.setRecordStatus(RecordStatusConstant.DELETED);
            investmentEntityDao.saveRecord(investment);

            response.setInvestment(null);
            response.setCreated(false);
            response.setMessage("Customer debit failed");
        }

        return response;
    }

    private boolean debitCustomerAccount(InvestmentEntity investment, MintBankAccountEntity bankAccount) {
        boolean response;
        String ref = investmentEntityDao.generateInvestmentTransactionRef();

        InvestmentTransactionEntity transaction = new InvestmentTransactionEntity();
        transaction.setInvestment(investment);
        transaction.setBankAccount(bankAccount);
        transaction.setTransactionAmount(investment.getAmountInvested());
        transaction.setTransactionReference(ref);
        transaction.setTransactionType(TransactionTypeConstant.DEBIT);
        transaction.setTransactionStatus(TransactionStatusConstant.PENDING);
        transaction.setFundingSource(FundingSourceTypeConstant.MINT_ACCOUNT);

        transaction = investmentTransactionEntityDao.saveRecord(transaction);

        MintFundTransferRequestCBS request = MintFundTransferRequestCBS.builder()
                .amount(transaction.getTransactionAmount())
                .debitAccountNumber(bankAccount.getAccountNumber())
                .creditAccountNumber("")
                .narration(constructInvestmentNarration(investment, ref))
                .transactionReference(ref)
                .build();

        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processMintFundTransfer(request);

        if (!msClientResponse.isSuccess() || msClientResponse.getStatusCode() != HttpStatus.OK.value() || msClientResponse.getData() == null) {
            String message = String.format("Investment Id: %s; transaction Id: %s ; message: %s", investment.getCode(), transaction.getTransactionReference(), msClientResponse.getMessage());
            systemIssueLogService.logIssue("Investment Funding Issue", "Customer investment funding failed", message);
            transaction.setTransactionStatus(TransactionStatusConstant.FAILED);
            response = false;
        } else {
            FundTransferResponseCBS responseCBS = msClientResponse.getData();
            transaction.setTransactionResponseCode(responseCBS.getResponseCode());
            transaction.setTransactionResponseMessage(responseCBS.getResponseMessage());
            transaction.setExternalReference(responseCBS.getBankOneReference());

            if ("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
                transaction.setTransactionStatus(TransactionStatusConstant.SUCCESSFUL);
                response = true;
            } else {
                transaction.setTransactionStatus(TransactionStatusConstant.FAILED);
                String message = String.format("Investment Id: %s; transaction Id: %s ; message: %s", investment.getCode(), transaction.getTransactionReference(), msClientResponse.getMessage());
                systemIssueLogService.logIssue("Investment Funding Issue", "Customer investment funding failed", message);
                response = false;
            }

        }
        investmentTransactionEntityDao.saveRecord(transaction);
        return response;
    }

    private String constructInvestmentNarration(InvestmentEntity investment, String reference) {
        String narration = String.format("IP-%s %s", investment.getCode(), reference);
        if (narration.length() > 61) {
            return narration.substring(0, 60);
        }
        return narration;
    }

}
