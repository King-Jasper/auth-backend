package com.mintfintech.savingsms.domain.services;

/**
 * Created by jnwanya on
 * Tue, 07 Apr, 2020
 */
public interface SystemIssueLogService {
    void logIssue(String emailSubject, String title, String detail);
}
