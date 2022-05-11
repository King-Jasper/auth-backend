package com.mintfintech.savingsms.usecase.data.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReferralDetailsResponse {

    private BigDecimal totalEarnings;

    private int numberOfCustomersReferred;

    private long customerAirtimeReward;

    private long userAirtimeReward;
}
