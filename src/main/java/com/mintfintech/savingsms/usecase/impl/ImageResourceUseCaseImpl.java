package com.mintfintech.savingsms.usecase.impl;

import com.mintfintech.savingsms.domain.dao.ResourceFileEntityDao;
import com.mintfintech.savingsms.domain.entities.ResourceFileEntity;
import com.mintfintech.savingsms.domain.models.cloudstorageservice.FileStorageRequest;
import com.mintfintech.savingsms.domain.models.cloudstorageservice.FileStorageResponse;
import com.mintfintech.savingsms.domain.services.CloudStorageService;
import com.mintfintech.savingsms.usecase.ImageResourceUseCase;
import com.mintfintech.savingsms.usecase.exceptions.BadRequestException;
import com.mintfintech.savingsms.usecase.exceptions.BusinessLogicConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImageResourceUseCaseImpl implements ImageResourceUseCase {

    private final ResourceFileEntityDao resourceFileEntityDao;
    private final CloudStorageService cloudStorageService;

    @Override
    public ResourceFileEntity createImage(String storageFolderName, MultipartFile file) {

        byte[] imageByteArray;
        String fileName = file.getOriginalFilename();
        try {
            imageByteArray = file.getBytes();
        } catch (Exception ex) {
            throw new BadRequestException("Unable to extract image file.");
        }
        if(fileName != null && !fileName.endsWith(".pdf")) {
            fileName = fileName+".pdf";
        }

        FileStorageRequest storageRequest = FileStorageRequest.builder()
                .fileData(imageByteArray)
                .privateFile(false)
                .folderName(storageFolderName)
                .fileName(fileName)
                .build();
        FileStorageResponse storageResponse = cloudStorageService.uploadResourceFile(storageRequest);
        if (!storageResponse.isSuccess()) {
            throw new BusinessLogicConflictException("Sorry, unable to create product image at the moment.");
        }
        ResourceFileEntity fileEntity = ResourceFileEntity.builder()
                .fileId(storageResponse.getFileId())
                .fileName(fileName)
                .fileSizeInKB(storageResponse.getFileSizeInKB())
                .remoteResource(true)
                .url(storageResponse.getUrl())
                .build();
        return resourceFileEntityDao.saveRecord(fileEntity);
    }
}
