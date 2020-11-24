package com.mintfintech.savingsms.infrastructure.web.models;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * Created by jnwanya on
 * Thu, 22 Oct, 2020
 */
@Data
public class OnlineFundingRequestJSON {
    @ApiModelProperty(notes = "Amount to be funded in naira. Minimum amount: N500", required = true)
    private long amount;
    @ApiModelProperty(notes = "Payment Gateway: PAYSTACK | FLUTTERWAVE", required = true)
    @NotEmpty
    @NotNull
    @Pattern(regexp = "(PAYSTACK|paystack|FLUTTERWAVE)")
    private String paymentGateway;
}
