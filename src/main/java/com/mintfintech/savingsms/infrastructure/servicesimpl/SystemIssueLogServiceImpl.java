package com.mintfintech.savingsms.infrastructure.servicesimpl;

import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.SystemIssueLogService;
import com.mintfintech.savingsms.usecase.data.events.outgoing.SystemIssueEmailEvent;
import com.mintfintech.savingsms.usecase.data.value_objects.EmailNotificationType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


import org.springframework.beans.factory.annotation.Value;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Tue, 07 Apr, 2020
 */
@Slf4j
@Named
public class SystemIssueLogServiceImpl implements SystemIssueLogService {

    private final ApplicationEventService applicationEventService;
    private final ApplicationProperty applicationProperty;

    @Value("${spring.application.name}")
    private String systemName;

    public SystemIssueLogServiceImpl(ApplicationEventService applicationEventService, ApplicationProperty applicationProperty) {
        this.applicationEventService = applicationEventService;
        this.applicationProperty = applicationProperty;
    }



    @Override
    public void logIssue(String emailTitle, String title, String detail) {
        String env = "";
        if(applicationProperty.isProductionEnvironment()) {
            env = "Production";
        }else if(applicationProperty.isStagingEnvironment()) {
            env = "Staging";
        }else {
            env = "Development";
        }
        title = String.format("%s - (%s:%s)", title, systemName, env);
        log.info("SYSTEM ISSUE: {}: DETAIL {}", title, detail);
        SystemIssueEmailEvent emailEvent = SystemIssueEmailEvent.builder()
                .detail(detail)
                .title(title)
                .recipient(applicationProperty.getSystemAdminEmail())
                .build();
        if(!StringUtils.isEmpty(emailTitle)) {
            emailEvent.setSubject(emailTitle);
        }
        applicationEventService.publishEvent(ApplicationEventService.EventType.EMAIL_SYSTEM_ISSUE_ALERT, new EventModel<>(emailEvent));
    }
}
