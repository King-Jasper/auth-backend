package com.mintfintech.savingsms.usecase.features.spend_and_save;

import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.request.SpendAndSaveWithdrawalRequest;

public interface WithdrawSpendAndSaveUseCase {
    String withdrawSavings(AuthenticatedUser authenticatedUser, SpendAndSaveWithdrawalRequest withdrawalRequest);

}
