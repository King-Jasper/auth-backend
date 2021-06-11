package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.models.accountsservice.PinValidationRequest;
import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;
import com.mintfintech.savingsms.domain.services.AccountsRestClient;
import com.mintfintech.savingsms.usecase.AccountAuthorisationUseCase;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import com.mintfintech.savingsms.usecase.exceptions.UnauthorisedException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountAuthorisationUseCaseImpl implements AccountAuthorisationUseCase {

    private final AccountsRestClient accountsRestClient;

    @Override
    public void validationTransactionPin(String pin) {

        PinValidationRequest request = PinValidationRequest.builder()
                .transactionPin(pin)
                .build();
        MsClientResponse<String> msClientResponse = accountsRestClient.validationTransactionPin(request);
        processResponse(msClientResponse);
    }

    private void processResponse(MsClientResponse<String> msClientResponse) {
        int responseStatusCode = msClientResponse.getStatusCode();
        if (responseStatusCode == HttpStatus.OK.value()) {
            return;
        }
        if (responseStatusCode == HttpStatus.BAD_REQUEST.value()) {
            throw new BadRequestException(msClientResponse.getMessage());
        }
        if (responseStatusCode == HttpStatus.UNAUTHORIZED.value()) {
            throw new UnauthorisedException(msClientResponse.getMessage());
        }
        if (responseStatusCode == HttpStatus.CONFLICT.value()) {
            throw new BusinessLogicConflictException(msClientResponse.getMessage());
        }
        throw new BusinessLogicConflictException("Sorry, unable to validate request at the moment. Please try again later.");
    }
}
