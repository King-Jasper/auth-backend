package com.mintfintech.savingsms.domain.models.cloudstorageservice;

import lombok.Data;

/**
 * Created by jnwanya on
 * Wed, 08 Apr, 2020
 */
@Data
public class FileStorageResponse {
    private String url;
    private String fileId;
    private long fileSizeInKB;
    private boolean success;
}
