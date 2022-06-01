package com.mintfintech.savingsms.usecase.features.referral_savings.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.dao.MintBankAccountEntityDao;
import com.mintfintech.savingsms.domain.dao.ReactHQReferralEntityDao;
import com.mintfintech.savingsms.domain.dao.SavingsGoalTransactionEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.domain.entities.ReactHQReferralEntity;
import com.mintfintech.savingsms.domain.entities.enums.BankAccountTypeConstant;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.models.corebankingservice.BalanceEnquiryResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.FundTransferResponseCBS;
import com.mintfintech.savingsms.domain.models.corebankingservice.MintFundTransferRequestCBS;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.CoreBankingServiceClient;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.usecase.UpdateBankAccountBalanceUseCase;
import com.mintfintech.savingsms.usecase.data.events.incoming.AccountCreditEvent;
import com.mintfintech.savingsms.usecase.data.events.outgoing.PushNotificationEvent;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.features.referral_savings.ReachHQTransactionUseCase;
import com.mintfintech.savingsms.utils.MoneyFormatterUtil;
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
    private final AppUserEntityDao appUserEntityDao;
    private final ApplicationEventService applicationEventService;

    @Override
    public void processCustomerDebit(AccountCreditEvent accountCreditEvent) {
        String accountNumber = accountCreditEvent.getAccountNumber();
        processCustomerDebit(accountNumber, false,  true);
    }

    @Override
    public boolean processCustomerDebit(String accountNumber, boolean createRecord, boolean canBeCredited) {
        boolean debitSuccess = false;
        ReactHQReferralEntity reactHQReferral;
        Optional<ReactHQReferralEntity> optional =  reactHQReferralEntityDao.findCustomerForDebit(accountNumber);
        if(!optional.isPresent()) {
            if(createRecord) {
               reactHQReferral = createRecord(accountNumber, canBeCredited);
            }else {
                System.out.println("RECORD NOT FOUND - "+accountNumber);
                return debitSuccess;
            }
        }else {
            reactHQReferral = optional.get();
        }
        if(reactHQReferral.isCustomerDebited()) {
            return debitSuccess;
        }
        BigDecimal amountForDebit = BigDecimal.valueOf(1000.00);
        MintAccountEntity customer = reactHQReferral.getCustomer();
        MintBankAccountEntity debitAccount = mintBankAccountEntityDao.getAccountByMintAccountAndAccountType(customer, BankAccountTypeConstant.CURRENT);
        debitAccount = updateBankAccountBalanceUseCase.processBalanceUpdate(debitAccount);
        if(amountForDebit.compareTo(debitAccount.getAvailableBalance()) > 0) {
            System.out.println("INSUFFICIENT BALANCE - "+accountNumber);
            return debitSuccess;
        }
        reactHQReferral.setDebitTrialCount(1);
        reactHQReferralEntityDao.saveRecord(reactHQReferral);
        MintFundTransferRequestCBS requestCBS = MintFundTransferRequestCBS.builder()
                .transactionReference(savingsGoalTransactionEntityDao.generateTransactionReference())
                .amount(amountForDebit)
                .debitAccountNumber(debitAccount.getAccountNumber())
                .narration("ReactHQ Certificate fee")
                .creditAccountNumber(applicationProperty.reactHQAccountNumber())
                .build();
        MsClientResponse<FundTransferResponseCBS> msClientResponse = coreBankingServiceClient.processMintFundTransfer(requestCBS);
        if(!msClientResponse.isSuccess()) {
            systemIssueLogService.logIssue("REACTHQ DEBIT FAILURE", "CUSTOMER DEBIT FAILURE", msClientResponse.toString());
            return debitSuccess;
        }
        FundTransferResponseCBS  responseCBS = msClientResponse.getData();
        reactHQReferral.setDebitResponseCode(responseCBS.getResponseCode());
        reactHQReferral.setDebitResponseMessage(responseCBS.getResponseMessage());
        if("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
            reactHQReferral.setCustomerDebited(true);
            debitSuccess = true;
        }else {
            systemIssueLogService.logIssue("REACTHQ DEBIT FAILURE", "CUSTOMER DEBIT FAILURE",responseCBS.toString());
        }
        reactHQReferralEntityDao.saveRecord(reactHQReferral);

        if("00".equalsIgnoreCase(responseCBS.getResponseCode())) {
            AppUserEntity user = appUserEntityDao.getRecordById(customer.getCreator().getId());
            String text = "Hi "+user.getFirstName()+", You have been debited N"+ MoneyFormatterUtil.priceWithDecimal(amountForDebit)+" for ReactHQ Certificate fee. Thanks for choosing Mintyn.";
            PushNotificationEvent pushNotificationEvent = new PushNotificationEvent("ReactHQ Certificate fee", text, user.getDeviceGcmNotificationToken());
            pushNotificationEvent.setUserId(user.getUserId());
            applicationEventService.publishEvent(ApplicationEventService.EventType.PUSH_NOTIFICATION_TOKEN, new EventModel<>(pushNotificationEvent));
        }
        return debitSuccess;
    }

    private ReactHQReferralEntity createRecord(String accountNumber, boolean canBeCredited) {
        Optional<MintBankAccountEntity> optional = mintBankAccountEntityDao.findByAccountNumber(accountNumber);
        if(!optional.isPresent()) {
            throw new BadRequestException("Bank account not found.");
        }
        MintBankAccountEntity bankAccount = optional.get();
        ReactHQReferralEntity referralEntity = ReactHQReferralEntity.builder()
                .customer(bankAccount.getMintAccount())
                .customerCredited(false)
                .registrationPlatform("")
                .customerDebited(false)
                .canBeCredited(canBeCredited)
                .build();
        return reactHQReferralEntityDao.saveRecord(referralEntity);
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
        if(referralEntity.getCanBeCredited() != null && !referralEntity.getCanBeCredited()) {
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
