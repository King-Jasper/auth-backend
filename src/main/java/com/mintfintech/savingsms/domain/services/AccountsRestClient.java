package com.mintfintech.savingsms.domain.services;

import com.mintfintech.savingsms.domain.models.accountsservice.PinValidationRequest;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.usecase.data.events.incoming.UserDetailUpdateEvent;

public interface AccountsRestClient {

    MsClientResponse<String> validationTransactionPin(PinValidationRequest validationRequest);
    MsClientResponse<UserDetailUpdateEvent> getUserDetails(String userId);


}
