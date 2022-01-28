package com.mintfintech.savingsms.usecase.features.spend_and_save;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.SpendAndSaveSetUpRequest;
import com.mintfintech.savingsms.usecase.data.response.SpendAndSaveResponse;

public interface CreateSpendAndSaveUseCase {
    SpendAndSaveResponse setUpSpendAndSave(AuthenticatedUser authenticatedUser, SpendAndSaveSetUpRequest setUpRequest);
}
