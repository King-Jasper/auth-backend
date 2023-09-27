package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.MintBankAccountEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.data.response.MintBankAccountResponse;

/**
 * Created by jnwanya on
 * Wed, 24 Nov, 2021
 */
public interface GetMintAccountUseCase {
    MintAccountEntity getMintAccount(AuthenticatedUser authenticatedUser);
    MintBankAccountEntity getMintBankAccount(MintAccountEntity accountEntity, String accountId);
    MintBankAccountResponse getMintBankAccountResponse(String accountNumber);
}
