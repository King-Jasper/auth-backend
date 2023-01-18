package com.mintfintech.savingsms.usecase.data.response;

import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Mon, 22 Jun, 2020
 */
@Data
public class PortalSavingsGoalResponse {
    @Builder.Default
    private String userId = "";
    @Builder.Default
    private String customerName = "";
    @Builder.Default
    private String accountId = "";
}
