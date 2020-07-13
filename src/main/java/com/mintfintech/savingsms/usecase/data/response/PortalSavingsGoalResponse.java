package com.mintfintech.savingsms.usecase.data.response;

import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import lombok.Data;

/**
 * Created by jnwanya on
 * Mon, 22 Jun, 2020
 */
@Data
public class PortalSavingsGoalResponse {
    private String userId;
    private String customerName;
    private String accountId;
}
