package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Builder
@Data
public class CorporateTransactionSearchRequest {

    private LocalDate fromDate;

    private LocalDate toDate;

    private String transactionType;

    private String approvalStatus;

}
