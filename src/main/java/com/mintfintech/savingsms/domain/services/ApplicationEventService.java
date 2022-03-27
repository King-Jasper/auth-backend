package com.mintfintech.savingsms.domain.services;


import com.mintfintech.savingsms.domain.models.EventModel;

/**
 * Created by jnwanya on
 * Thu, 06 Feb, 2020
 */
public interface ApplicationEventService {
    void publishEvent(EventType eventType, EventModel<?> domain);

    enum EventType {
        EMAIL_LOAN_PROFILE_CREATION("com.mintfintech.services.events.notification.email.loan-profile-creation"),
        EMAIL_LOAN_PROFILE_APPROVED("com.mintfintech.services.events.notification.email.loan-profile-approved"),
        EMAIL_LOAN_PROFILE_DECLINED("com.mintfintech.services.events.notification.email.loan-profile-declined"),
        EMAIL_LOAN_REQUEST_SUCCESS("com.mintfintech.services.events.notification.email.loan-request-success"),
        EMAIL_LOAN_REQUEST_APPROVED("com.mintfintech.services.events.notification.email.loan-request-approved"),
        EMAIL_LOAN_REQUEST_DECLINED("com.mintfintech.services.events.notification.email.loan-request-declined"),
        EMAIL_LOAN_REPAYMENT_REMINDER("com.mintfintech.services.events.notification.email.loan-repayment-reminder"),
        EMAIL_LOAN_REPAYMENT_SUCCESS("com.mintfintech.services.events.notification.email.loan-repayment-success"),
        EMAIL_LOAN_REPAYMENT_FAILURE("com.mintfintech.services.events.notification.email.loan-repayment-failure"),
        EMAIL_LOAN_PARTIAL_REPAYMENT_SUCCESS("com.mintfintech.services.events.notification.email.loan-partial-repayment-success"),
        EMAIL_LOAN_REQUEST_ADMIN("com.mintfintech.services.events.notification.email.loan-request-admin"),
        EMAIL_LOAN_PROFILE_UPDATE_ADMIN("com.mintfintech.services.events.notification.email.loan-profile-update-admin"),
        EMAIL_SYSTEM_ISSUE_ALERT("com.mintfintech.services.events.notification.email.system-internal-issue-alert"),
        EMAIL_SAVINGS_GOAL_FUNDING_FAILURE("com.mintfintech.services.events.notification.email.savings-funding-failure"),
        EMAIL_SAVINGS_GOAL_FUNDING_SUCCESS("com.mintfintech.services.events.notification.email.savings-funding-success"),
        EMAIL_SAVINGS_GOAL_WITHDRAWAL("com.mintfintech.services.events.notification.email.savings-withdrawal"),
        SAVING_GOAL_CREATION("com.mintfintech.saving-service.events.saving-goal-creation"),
        SAVING_GOAL_BALANCE_UPDATE("com.mintfintech.saving-service.events.saving-goal-balance-update"),
        MINT_TRANSACTION_LOG("com.mintfintech.fund-transaction-service.events.transaction-log"),
        PUSH_NOTIFICATION_TOKEN_ACCOUNTS("com.mintfintech.services.events.accounts-service.gcm"),
        PUSH_NOTIFICATION_TOKEN("com.mintfintech.services.events.notification.gcm.token"),
        SMS_NOTIFICATION("com.mintfintech.services.events.accounts-service.sms"),
        MISSING_ACCOUNT_RECORD("com.mintfintech.accounts-service.events.missing-account-records"),
        EMPLOYMENT_INFORMATION_UPDATE("com.mintfintech.savings-service.events.employment-info-update"),
        MISSING_BANK_ACCOUNT_RECORD("com.mintfintech.accounts-service.events.missing-account-records.bank-account-id"),
        APPLICATION_AUDIT_TRAIL("com.mintfintech.report-service.events.audit-trail"),
        INVESTMENT_CREATION("com.mintfintech.saving-service.events.notification.email.investment-creation"),
        INVESTMENT_LIQUIDATION_SUCCESS("com.mintfintech.saving-service.events.notification.email.investment-liquidation"),
        INVESTMENT_MATURITY("com.mintfintech.saving-service.events.notification.email.investment-maturity"),
        INVESTMENT_FUNDING_SUCCESS("com.mintfintech.saving-service.events.notification.email.investment-funding-success"),
        CORPORATE_INVESTMENT_REQUEST("com.mintfintech.saving-service.events.corporate-investment-request"),
        CORPORATE_INVESTMENT_CREATION("com.mintfintech.savings-services.events.notification.email.corporate-investment-creation"),
        PENDING_CORPORATE_INVESTMENT("com.mintfintech.savings-services.events.notification.email.pending-corporate-investment"),
        CORPORATE_INVESTMENT_TOP_UP("com.mintfintech.savings-services.events.notification.email.corporate-investment-top-up"),
        CORPORATE_INVESTMENT_LIQUIDATION("com.mintfintech.savings-services.events.notification.email.corporate-investment-liquidation"),
        DECLINED_CORPORATE_INVESTMENT("com.mintfintech.savings-services.events.notification.email.declined-corporate-investment"),
        AFFILIATE_MARKETING("com.mintfintech.services.events.affiliate-service.affiliate-marketing-referral");
        private final String topic;

        EventType(String topic) {
            this.topic = topic;
        }

        public String getTopic() {
            return topic;
        }
    }
}
