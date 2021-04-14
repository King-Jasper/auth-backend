package com.mintfintech.savingsms.usecase.data.request;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
@Builder
public class EmploymentDetailCreationRequest {

    private BigDecimal monthlyIncome;

    private String organizationName;

    private String organizationUrl;

    private String employerAddress;

    private String employerEmail;

    private String employerPhoneNo;

    private String workEmail;

    private MultipartFile employmentLetter;

    private double loanAmount;

    private String creditAccountId;
}
