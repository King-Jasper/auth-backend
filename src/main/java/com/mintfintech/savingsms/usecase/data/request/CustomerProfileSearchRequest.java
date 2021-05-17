package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CustomerProfileSearchRequest {

    private LocalDate fromDate;
    private LocalDate toDate;
    private String verificationStatus;
}
