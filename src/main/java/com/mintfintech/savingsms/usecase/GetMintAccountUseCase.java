package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;

/**
 * Created by jnwanya on
 * Wed, 24 Nov, 2021
 */
public interface GetMintAccountUseCase {
    MintAccountEntity getMintAccount(AuthenticatedUser authenticatedUser);
}
