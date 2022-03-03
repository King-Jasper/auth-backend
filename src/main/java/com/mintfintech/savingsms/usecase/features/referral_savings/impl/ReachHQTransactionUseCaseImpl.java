package com.mintfintech.savingsms.usecase.features.referral_savings.impl;

import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.ReactHQReferralEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.ReactHQReferralEntity;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;
import com.mintfintech.savingsms.domain.models.corebankingservice.BalanceEnquiryResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.MintFundTransferRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.incoming.AccountCreditEvent;
import com.mintfintech.savingsms.usecase.features.referral_savings.ReachHQTransactionUseCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Thu, 03 Mar, 2022
 */
@Slf4j
@Named
@AllArgsConstructor
public class ReachHQTransactionUseCaseImpl implements ReachHQTransactionUseCase {
    private final ReactHQReferralEntityDao reactHQReferralEntityDao;
    private final MintBankAccountEntityDao mintBankAccountEntityDao;
    private final ApplicationProperty applicationProperty;
    private final UpdateBankAccountBalanceUseCase updateBankAccountBalanceUseCase;
    private final CoreBankingServiceClient coreBankingServiceClient;
    private final SavingsGoalTransactionEntityDao savingsGoalTransactionEntityDao;
    private final SystemIssueLogService systemIssueLogService;

    @Override
    public void processCustomerDebit(AccountCreditEvent accountCreditEvent) {
        String accountNumber = accountCreditEvent.getAccountNumber();
        processCustomerDebit(accountNumber);
    }

    @Override
    public void processCustomerDebit(String accountNumber) {
        Optional<ReactHQReferralEntity> optional =  reactHQReferralEntityDao.findCustomerForDebit(accountNumber);
        if(!optional.isPresent()) {
            return;
        }
        ReactHQReferralEntity reactHQReferral = optional.get();
        if(!reactHQReferral.isCustomerDebited()) {
            return;
        }
        BigDecimal amountForDebit = BigDecimal.valueOf(1000.00);
        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(reactHQReferral.getCustomer(), BankAccountTypeConstant.CURRENT);
        debitAccount = updateBankAccountBalanceUseCase.processBalanceUpdate(debitAccount);
        if(amountForDebit.compareTo(debitAccount.getAvailableBalance()) > 0) {
            log.info("Not enough balance");
        }
        reactHQReferral.setDebitTrialCount(1);
        reactHQReferralEntityDao.saveRecord(reactHQReferral);
        MintFundTransferRequestCBS requestCBS = MintFundTransferRequestCBS.builder()
                .transactionReference(savingsGoalTransactionEntityDao.generateTransactionReference())
                .amount(amountForDebit)
                .debitAccountNumber(debitAccount.getAccountNumber())
                .narration("Course fee - ReactHQ")
                .creditAccountNumber(applicationProperty.reactHQAccountNumber())
                .build();
        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processMintFundTransfer(requestCBS);
        if(!msClientResponse.isSuccess()) {
            systemIssueLogService.logIssue("REACTHQ DEBIT FAILURE", "CUSTOMER DEBIT FAILURE", msClientResponse.toString());
            return;
        }
        FundTransferResponseCBS  responseCBS = msClientResponse.getData();
        reactHQReferral.setDebitResponseCode(responseCBS.getResponseCode());
        reactHQReferral.setDebitResponseMessage(responseCBS.getResponseMessage());
        if("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
            reactHQReferral.setCustomerDebited(true);
        }else {
            systemIssueLogService.logIssue("REACTHQ DEBIT FAILURE", "CUSTOMER DEBIT FAILURE",responseCBS.toString());
        }
        reactHQReferralEntityDao.saveRecord(reactHQReferral);
    }

    @Override
    public void processCustomerCredit() {
        long count = reactHQReferralEntityDao.countCustomerSupported();
        if(count >= 300 || count == 0) {
            return;
        }
        int size = 10;
        if(count + size > 300) {
            size = (int) (300 - count);
        }
        BigDecimal amountForCredit = BigDecimal.valueOf(500.00);
        amountForCredit = amountForCredit.multiply(BigDecimal.valueOf(size));

        String debitAccountNumber = applicationProperty.getMintBusinessDevelopmentAccountNumber();
        MsClientResponse<BalanceEnquiryResponseCBS> balanceResponse = coreBankingServiceClient.retrieveAccountBalance(debitAccountNumber);
        if(!balanceResponse.isSuccess()) {
            return;
        }
        if(balanceResponse.getData().getAvailableBalance().compareTo(amountForCredit) < 0) {
            systemIssueLogService.logIssue("REACTHQ CREDIT FAILURE", "CUSTOMER CREDIT FAILURE - INSUFFICIENT FUND", balanceResponse.toString());
            return;
        }

        List<ReactHQReferralEntity> referrals = reactHQReferralEntityDao.getCustomerForFundSupport(size);
        for(ReactHQReferralEntity referral : referrals) {
            processRefund(referral);
        }
    }

    private void processRefund(ReactHQReferralEntity referralEntity) {
        if(referralEntity.isCustomerCredited()) {
            return;
        }
        MintBankAccountEntity creditAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(referralEntity.getCustomer(), BankAccountTypeConstant.CURRENT);
        BigDecimal amountForCredit = BigDecimal.valueOf(500.00);
        referralEntity.setCreditTrialCount(1);
        reactHQReferralEntityDao.saveRecord(referralEntity);

        String debitAccountNumber = applicationProperty.getMintBusinessDevelopmentAccountNumber();

        MintFundTransferRequestCBS requestCBS = MintFundTransferRequestCBS.builder()
                .transactionReference(savingsGoalTransactionEntityDao.generateTransactionReference())
                .amount(amountForCredit)
                .debitAccountNumber(debitAccountNumber)
                .narration("Course fee refund - ReactHQ")
                .creditAccountNumber(creditAccount.getAccountNumber())
                .build();
        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processMintFundTransfer(requestCBS);
        if(!msClientResponse.isSuccess()) {
            systemIssueLogService.logIssue("REACTHQ CREDIT FAILURE", "CUSTOMER CREDIT FAILURE", msClientResponse.toString());
            return;
        }
        FundTransferResponseCBS  responseCBS = msClientResponse.getData();
        referralEntity.setCreditResponseCode(responseCBS.getResponseCode());
        referralEntity.setCreditResponseMessage(responseCBS.getResponseMessage());
        if("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
            referralEntity.setCustomerCredited(true);
        }else {
            systemIssueLogService.logIssue("REACTHQ CREDIT FAILURE", "CUSTOMER CREDIT FAILURE",responseCBS.toString());
        }
        reactHQReferralEntityDao.saveRecord(referralEntity);
    }
}
