package com.mintfintech.savingsms.domain.services;

import com.mintfintech.savingsms.domain.models.cloudstorageservice.FileStorageRequest;
import com.mintfintech.savingsms.domain.models.cloudstorageservice.FileStorageResponse;

/**
 * Created by jnwanya on
 * Wed, 08 Apr, 2020
 */
public interface CloudStorageService {
    FileStorageResponse uploadResourceFile(FileStorageRequest request);
    void deleteResourceFile(String fileId);
    String generateURLForPrivateFileAccess(String fileId);
    byte[] downloadFile(String fileKey);
}
