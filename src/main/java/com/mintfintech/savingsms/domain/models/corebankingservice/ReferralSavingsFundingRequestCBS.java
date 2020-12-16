package com.mintfintech.savingsms.domain.models.corebankingservice;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Tue, 15 Dec, 2020
 */
@Data
@Builder
public class ReferralSavingsFundingRequestCBS {
    private String goalId;
    private String goalName;
    private BigDecimal amount;
    private String narration;
    private String reference;
}
