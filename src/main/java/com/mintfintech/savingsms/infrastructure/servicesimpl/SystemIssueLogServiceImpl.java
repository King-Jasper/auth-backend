package com.mintfintech.savingsms.infrastructure.servicesimpl;

import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Tue, 07 Apr, 2020
 */
@Slf4j
@Named
public class SystemIssueLogServiceImpl implements SystemIssueLogService {

    @Override
    public void logIssue(String title, String detail) {
       log.warn("ISSUE LOG: {}, DETAIL: {}", title, detail);
    }
}
