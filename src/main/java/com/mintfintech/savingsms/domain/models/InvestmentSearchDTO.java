package com.mintfintech.savingsms.domain.models;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class InvestmentSearchDTO {

    private LocalDateTime startFromDate;
    private LocalDateTime startToDate;
    private InvestmentStatusConstant investmentStatus;
    private MintAccountEntity account;
    private InvestmentTypeConstant investmentType;
    private String customerName;
    private LocalDateTime matureFromDate;
    private LocalDateTime matureToDate;
    private boolean completedRecords;
    private int duration;
}
