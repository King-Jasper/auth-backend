package com.mintfintech.savingsms.usecase.data.events.outgoing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Thu, 22 Apr, 2021
 */
@Data
@Builder
public class EmploymentInfoUpdateEvent {
    private String userId;
    private BigDecimal monthlySalary;
    private String organizationName;
    private String organizationUrl;
    private String employerEmail;
    private String employerAddress;
    private String employerPhoneNumber;
    private String workEmail;
    private String employmentLetterFileUrl;
    private String employmentLetterFileId;
    private String employmentLetterFileName;
    private long employmentLetterFileSize;
}
