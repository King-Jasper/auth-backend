package com.mintfintech.savingsms.usecase.data.response;

import com.mintfintech.savingsms.usecase.models.SavingsGoalModel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 22 Jun, 2020
 */
@Data
public class PortalSavingsGoalResponse extends SavingsGoalModel {
    private String userId;
    private String customerName;
    private String accountId;
}
