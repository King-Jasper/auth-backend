package com.mintfintech.savingsms.domain.services;


import com.mintfintech.savingsms.domain.models.EventModel;

/**
 * Created by jnwanya on
 * Thu, 06 Feb, 2020
 */
public interface ApplicationEventService {
    void publishEvent(EventType eventType, EventModel<?> domain);

    enum EventType { //savings-goal-funding-failure
        EMAIL_SYSTEM_ISSUE_ALERT("com.mintfintech.services.events.notification.email.system-internal-issue-alert"),
        EMAIL_SAVINGS_GOAL_FUNDING_FAILURE("com.mintfintech.services.events.notification.email.savings-funding-failure"),
        SAVING_GOAL_CREATION("com.mintfintech.saving-service.events.saving-goal-creation"),
        SAVING_GOAL_BALANCE_UPDATE("com.mintfintech.saving-service.events.saving-goal-balance-update"),
        MINT_TRANSACTION_LOG("com.mintfintech.fund-transaction-service.events.transaction-log"),
        NEW_EMAIL_NOTIFICATION("com.mintfintech.services.events.notification.email"),
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
