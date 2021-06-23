package com.mintfintech.savingsms.domain.models.reports;

import java.math.BigDecimal;

/**
 * Created by jnwanya on
 * Mon, 15 Mar, 2021
 */
public class AmountModel {
    private BigDecimal amount;
    public AmountModel(BigDecimal amount) {
      this.amount = amount;
    }
    public BigDecimal getAmount() {
        return amount;
    }
}
