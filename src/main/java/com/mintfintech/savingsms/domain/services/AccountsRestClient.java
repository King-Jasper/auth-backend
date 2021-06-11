package com.mintfintech.savingsms.domain.services;

import com.mintfintech.savingsms.domain.models.accountsservice.PinValidationRequest;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;

public interface AccountsRestClient {

    MsClientResponse<String> validationTransactionPin(PinValidationRequest validationRequest);

}
