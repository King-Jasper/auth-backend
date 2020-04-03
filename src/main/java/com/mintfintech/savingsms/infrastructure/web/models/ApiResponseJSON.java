package com.mintfintech.savingsms.infrastructure.web.models;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */
@Data
@Builder
public class ApiResponseJSON<T> {
    public ApiResponseJSON(String message) {
        this.message = message;
        this.data = null;
    }
    public ApiResponseJSON(String message, T data) {
        this.message = message;
        this.data = data;
    }

    private String message;
    private T data;
}
