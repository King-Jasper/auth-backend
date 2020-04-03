package com.mintfintech.savingsms.domain.models.restclient;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Sat, 01 Feb, 2020
 */
@Data
@Builder
public class MsClientRequest {
    private String data;
    private String serviceUrl;
}
