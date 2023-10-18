package com.mintfintech.savingsms.usecase.data.response;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Wed, 27 Sep, 2023
 */
@Data
@Builder
public class MintBankAccountResponse {
    private String accountId;
    private String accountName;
    private String accountNumber;
}
