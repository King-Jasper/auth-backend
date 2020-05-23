package com.mintfintech.savingsms.usecase.data.value_objects;

/**
 * Created by jnwanya on
 * Mon, 17 Feb, 2020
 */
public enum EmailNotificationType {
    SYSTEM_ISSUE_ALERT("mint_system-internal-issue-alert"),
    SAVINGS_GOAL_FUNDING_FAILURE("mint_savings-goal-funding-failure");
    private final String name;
    EmailNotificationType(String name){
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
