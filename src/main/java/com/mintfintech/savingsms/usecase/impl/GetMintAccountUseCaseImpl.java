package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.MintAccountEntityDao;
import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.models.EventModel;
import com.mintfintech.savingsms.domain.services.ApplicationEventService;
import com.mintfintech.savingsms.infrastructure.web.security.AuthenticatedUser;
import com.mintfintech.savingsms.usecase.GetMintAccountUseCase;
import com.mintfintech.savingsms.usecase.data.events.outgoing.MintAccountRecordRequestEvent;
import com.mintfintech.savingsms.usecase.exceptions.UnauthorisedException;
import lombok.AllArgsConstructor;

import javax.inject.Named;
import java.util.Collections;
import java.util.Optional;

/**
 * Created by jnwanya on
 * Wed, 24 Nov, 2021
 */
@Named
@AllArgsConstructor
public class GetMintAccountUseCaseImpl implements GetMintAccountUseCase {

    private final MintAccountEntityDao mintAccountEntityDao;
    private final ApplicationEventService applicationEventService;
    
    @Override
    public MintAccountEntity getMintAccount(AuthenticatedUser authenticatedUser) {
        Optional<MintAccountEntity> accountEntityOptional = mintAccountEntityDao.findAccountByAccountId(authenticatedUser.getAccountId());
        if(!accountEntityOptional.isPresent()) {
            MintAccountRecordRequestEvent requestEvent = MintAccountRecordRequestEvent.builder()
                    .topicNameSuffix("savings-service")
                    .accountIds(Collections.singletonList(authenticatedUser.getAccountId()))
                    .build();
            applicationEventService.publishEvent(ApplicationEventService.EventType.MISSING_ACCOUNT_RECORD, new EventModel<>(requestEvent));
            throw new UnauthorisedException("Invalid accountId.");
        }
        return accountEntityOptional.get();
    }
}
