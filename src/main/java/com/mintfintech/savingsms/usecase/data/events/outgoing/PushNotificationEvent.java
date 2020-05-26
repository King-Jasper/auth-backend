package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

/**
 * Created by jnwanya on
 * Mon, 25 May, 2020
 */
@Data
public class PushNotificationEvent {
    private ArrayList<String> tokens = new ArrayList<>();
    private String topic;
    private String message;
    private String userId;

    public PushNotificationEvent() {}

    public PushNotificationEvent(String message, String tokenId) {
        this.message = message;
        if(!StringUtils.isEmpty(tokenId)){
            this.tokens.add(tokenId);
        }
    }

    public void addToken(String tokenId) {
        this.tokens.add(tokenId);
    }
}
