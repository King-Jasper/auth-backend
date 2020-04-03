package com.mintfintech.savingsms.domain.models.restclient;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Sat, 01 Feb, 2020
 */
@Data
@Builder
public class MsClientResponse<T> {
    private boolean success;
    private int statusCode;
    private String message;
    private T data;
}
