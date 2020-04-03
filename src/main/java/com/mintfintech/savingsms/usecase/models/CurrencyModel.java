package com.mintfintech.savingsms.usecase.models;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
@Data
@Builder
public class CurrencyModel {
    private String symbol;
    private String code;
    private String name;
}
