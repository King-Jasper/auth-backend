package com.mintfintech.savingsms.usecase.models;

import lombok.Data;

@Data
public class EmploymentInformationModel {

    private double monthlyIncome;

    private String organizationName;

    private String organizationUrl;

    private String employerAddress;

    private String employerEmail;

    private String employerPhoneNo;

    private String workEmail;

    private String employmentLetterUrl;

    private String verified;

    private String rejectionReason;

}
