package com.mintfintech.savingsms.domain.services;

import com.mintfintech.savingsms.domain.services.EnvironmentService;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
@Named
public class ApplicationProperty {

    private EnvironmentService environmentService;
    public ApplicationProperty(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    public String getClientTokenSecretKey() {
        return environmentService.getVariable("token.client.secret-key", "");
    }

    public String getMicroserviceTokenSecretKey() {
        return environmentService.getVariable("token.ms.secret-key", "");
    }

    public int getMicroServiceTokenExpiryTimeInMinutes() {
        return Integer.parseInt(environmentService.getVariable("token.ms.expiry-time.mins", "5"));
    }

    public String getCoreBankingServiceName() {
        return environmentService.getVariable("microservices.core-banking.service-name", "");
    }
    public String getAccountsServiceName() {
        return environmentService.getVariable("microservices.accounts.service-name", "");
    }

    public int savingsMinimumNumberOfDaysForWithdrawal() {
        return Integer.parseInt(environmentService.getVariable("savings-goal.minimum-days-for-withdrawal", "30"));
    }

   /* public String getMintBankCode() {
        return environmentService.getVariable("mint.bank-code", "50304");
    }

    public String getSupportPhoneNumber() {
        return environmentService.getVariable("mint.support-phonenumber", "08067507380");
    }

    public int getTransactionStatusUpdateStartMinutes() {
        return Integer.parseInt(environmentService.getVariable("fund-transfer.status-update.start-after-minutes", "15"));
    }
    public int getTransactionStatusUpdateStopMinutes() {
        return Integer.parseInt(environmentService.getVariable("fund-transfer.status-update.stop-after-minutes", "90"));
    }*/
}
