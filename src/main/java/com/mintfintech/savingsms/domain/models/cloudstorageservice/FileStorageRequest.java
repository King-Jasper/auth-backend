package com.mintfintech.savingsms.domain.models.cloudstorageservice;

import lombok.Builder;
import lombok.Data;

/**
 * Created by jnwanya on
 * Wed, 08 Apr, 2020
 */
@Builder
@Data
public class FileStorageRequest {
    private boolean privateFile;
    private String fileName;
    private byte[] fileData;
    private String folderName;
}
