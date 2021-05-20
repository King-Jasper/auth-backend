package com.mintfintech.savingsms.domain.models;

import com.mintfintech.savingsms.domain.entities.MintAccountEntity;
import com.mintfintech.savingsms.domain.entities.enums.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InvestmentSearchDTO {

    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private InvestmentStatusConstant investmentStatus;
    private MintAccountEntity account;
    private InvestmentTypeConstant investmentType;
}
