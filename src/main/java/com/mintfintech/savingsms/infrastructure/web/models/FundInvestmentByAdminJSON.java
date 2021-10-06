package com.mintfintech.savingsms.infrastructure.web.models;

import com.mintfintech.savingsms.usecase.data.request.InvestmentFundingRequest;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
@Data
public class FundInvestmentByAdminJSON {
        @NotEmpty(message = "Customer's investment code is required.")
        private String investmentCode;
        @NotEmpty(message = "Customer's debit Account is required.")
        private String debitAccountId;
        @NotNull(message = "Amount is required.")
        private BigDecimal amount;

        public InvestmentFundingRequest toRequest() {
            return InvestmentFundingRequest.builder()
                    .investmentCode(investmentCode)
                    .debitAccountId(debitAccountId)
                    .amount(amount)
                    .build();
        }
}
