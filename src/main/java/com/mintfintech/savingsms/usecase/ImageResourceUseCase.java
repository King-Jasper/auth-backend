package com.mintfintech.savingsms.usecase;

import com.mintfintech.savingsms.domain.entities.ResourceFileEntity;
import org.springframework.web.multipart.MultipartFile;

public interface ImageResourceUseCase {
    ResourceFileEntity createImage(String storageFolderName, MultipartFile file);
}
