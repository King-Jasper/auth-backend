package com.mintfintech.savingsms.domain.services;

import com.mintfintech.savingsms.domain.services.EnvironmentService;

import javax.inject.Named;
import java.util.Arrays;

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
        return Integer.parseInt(environmentService.getVariable("savings-goal.minimum-days-for-withdrawal", "10"));
    }

    public double savingsInterestPercentageDeduction() {
        return Double.parseDouble(environmentService.getVariable("savings-goal.interest-percentage-deduction", "50"));
    }

    public int investmentMinimumLiquidationDays() {
        return Integer.parseInt(environmentService.getVariable("investment.min-liquidation-days", "15"));
    }

    public double getNipTransactionInterest() {
        return Double.parseDouble(environmentService.getVariable("mint-interest.nip-transaction", "0.0"));
    }

    public double getMaxLiquidateRate() {
        return Double.parseDouble(environmentService.getVariable("investment.max-liquidate-rate", "0.0"));
    }

    public long getReferralRewardAmount() {
        return Long.parseLong(environmentService.getVariable("mint.referral-reward-amount", "200"));
    }
    public long getReferredRewardAmount() {
        return Long.parseLong(environmentService.getVariable("mint.referred-reward-amount", "200"));
    }

    public String getSystemAdminEmail() {
        return environmentService.getVariable("mint.email.system-admin");
    }

    public String getLoanAdminEmail() {
        return environmentService.getVariable("mint.email.loan-admin", "justin.nwanya@bankwithmint.com");
    }


    public boolean isDevelopmentEnvironment() {
        return Arrays.stream(environmentService.getActiveProfiles()).anyMatch(evn -> evn.equalsIgnoreCase("dev"));
    }

    public boolean isStagingEnvironment() {
        return Arrays.stream(environmentService.getActiveProfiles()).anyMatch(evn -> evn.equalsIgnoreCase("staging"));
    }

    public boolean isProductionEnvironment() {
        return Arrays.stream(environmentService.getActiveProfiles()).anyMatch(evn -> evn.equalsIgnoreCase("prod"));
    }

    public boolean isLiveEnvironment() {
        return isStagingEnvironment() || isProductionEnvironment();
    }

    public boolean isKubernetesEnvironment() {
        return Arrays.stream(environmentService.getActiveProfiles()).anyMatch(evn -> evn.equalsIgnoreCase("kubernetes"));
    }

    public double getFileUploadMaximumSize() {
        return Double.parseDouble(environmentService.getVariable("file-upload-maximum-size-in-mb"));
    }

    public double getPayDayMaxLoanPercentAmount() {
        return Double.parseDouble(environmentService.getVariable("loan.pay-day.max-percent-amount"));
    }

    public double getPayDayLoanInterestRate() {
        return Double.parseDouble(environmentService.getVariable("loan.pay-day.interest-rate"));
    }

    public long getPayDayLoanMaxTenor() {
        return Long.parseLong(environmentService.getVariable("loan.pay-day.max-tenor-days"));
    }

    /*public double getBusinessLoanInterestRate() {
        return Double.parseDouble(environmentService.getVariable("loan.business-loan.interest-rate"));
    }*/

    public String getAmazonS3AccessKey(){
        return environmentService.getVariable("amazon.accessKey");
    }
    public String getAmazonS3SecretKey(){
        return environmentService.getVariable("amazon.secretKey");
    }

    public String getAmazonS3BucketName(){ return environmentService.getVariable("amazon.bucketName"); }

    public int getAmazonPrivateUrlExpirationTimeInMinutes(){
        return Integer.parseInt(environmentService.getVariable("amazon.private-file.access-expiry-time-in-minutes", "10"));
    }

    public String getAmazonS3Region(){
        return environmentService.getVariable("amazon.region");
    }

    public String reactHQAccountNumber() {
        return environmentService.getVariable("referral.reacthq.account-number");
    }
    public String getMintBusinessDevelopmentAccountNumber() {
        return environmentService.getVariable("referral.business-development.account-number");
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
