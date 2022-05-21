package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.AppUserEntityDao;
import com.mintfintech.savingsms.domain.entities.AppUserEntity;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.usecase.PushNotificationService;
import com.mintfintech.savingsms.usecase.data.events.outgoing.PushNotificationEvent;
import lombok.AllArgsConstructor;
import org.hibernate.Hibernate;

import javax.inject.Named;

/**
 * Created by jnwanya on
 * Thu, 03 Mar, 2022
 */
@Named
@AllArgsConstructor
public class PushNotificationServiceImpl implements PushNotificationService {
    private final AppUserEntityDao appUserEntityDao;
    private final ApplicationEventService applicationEventService;

    @Override
    public void sendMessage(AppUserEntity user, String messageTitle, String messageBody) {
        if(!Hibernate.isInitialized(user)) {
            user = appUserEntityDao.getRecordById(user.getId());
        }
        if(!user.isGcmNotificationEnabled()) {
            return;
        }
        if(!"".equalsIgnoreCase(user.getDeviceGcmNotificationToken())) {
            PushNotificationEvent pushNotificationEvent = new PushNotificationEvent(messageTitle, messageBody, user.getDeviceGcmNotificationToken());
            pushNotificationEvent.setUserId(user.getUserId());
            if (user.getDeviceGcmNotificationToken() == null) {
                applicationEventService.publishEvent(ApplicationEventService.EventType.PUSH_NOTIFICATION_TOKEN_ACCOUNTS, new EventModel<>(pushNotificationEvent));
                user.setDeviceGcmNotificationToken("");
                appUserEntityDao.saveRecord(user);
            } else {
                applicationEventService.publishEvent(ApplicationEventService.EventType.PUSH_NOTIFICATION_TOKEN, new EventModel<>(pushNotificationEvent));
            }
        }
    }
}
