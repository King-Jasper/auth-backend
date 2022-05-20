package com.mintfintech.savingsms.usecase;


import com.mintfintech.savingsms.domain.entities.AppUserEntity;
/**
 * Created by jnwanya on
 * Thu, 03 Mar, 2022
 */
public interface PushNotificationService {
    void sendMessage(AppUserEntity appUser, String messageTitle, String messageBody);
}
