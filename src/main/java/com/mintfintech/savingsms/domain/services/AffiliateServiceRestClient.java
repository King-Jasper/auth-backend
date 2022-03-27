package com.mintfintech.savingsms.domain.services;

import com.mintfintech.savingsms.domain.models.restclient.MsClientResponse;

public interface AffiliateServiceRestClient {
    MsClientResponse<String> validateReferralCode(String code);
}
