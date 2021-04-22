package com.mintfintech.savingsms.domain.services;


import com.mintfintech.savingsms.domain.models.EventModel;

/**
 * Created by jnwanya on
 * Thu, 06 Feb, 2020
 */
public interface ApplicationEventService {
    void publishEvent(EventType eventType, EventModel<?> domain);
//
    enum EventType {
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
        APPLICATION_AUDIT_TRAIL("com.mintfintech.report-service.events.audit-trail");
        private final String topic;

        EventType(String topic) {
            this.topic = topic;
        }

        public String getTopic() {
            return topic;
        }
    }
}
