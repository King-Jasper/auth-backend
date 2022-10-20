package com.mintfintech.savingsms.infrastructure.servicesimpl;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import com.mintfintech.savingsms.domain.models.cloudstorageservice.FileStorageRequest;
import com.mintfintech.savingsms.domain.models.cloudstorageservice.FileStorageResponse;
import com.mintfintech.savingsms.domain.services.ApplicationProperty;
import com.mintfintech.savingsms.domain.services.CloudStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import javax.inject.Named;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Named
public class AmazonCloudStorageService implements CloudStorageService {
    private AmazonS3 s3Client;
    private final ApplicationProperty applicationProperty;
    public AmazonCloudStorageService(ApplicationProperty applicationProperty) {
        this.applicationProperty = applicationProperty;
    }

    @PostConstruct
    private void initializeAmazon() {
        AWSCredentials credentials = new BasicAWSCredentials(applicationProperty.getAmazonS3AccessKey(), applicationProperty.getAmazonS3SecretKey());
        s3Client = AmazonS3ClientBuilder.standard()
               // .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(applicationProperty.getAmazonS3Region())
                .build();
    }

    @Override
    public FileStorageResponse uploadResourceFile(FileStorageRequest request) {

        String uniqueFileName = generateFileName(request.getFileName());
        String endPoint = String.format("https://%s.s3.%s.amazonaws.com/", applicationProperty.getAmazonS3BucketName(), applicationProperty.getAmazonS3Region());
        String folderName = request.getFolderName();
        String folderFileName = StringUtils.isEmpty(folderName) ? "general/" + uniqueFileName : folderName + "/" + uniqueFileName;
        FileStorageResponse fileStorageResponse = new FileStorageResponse();
        fileStorageResponse.setSuccess(false);
        File file = null;
        try {
            String fileUrl = endPoint + folderFileName;
            file = convertByteArrayToFile(request.getFileData(),request.getFileName());
            long fileSize =  file.length() / 1024;
            if(request.isPrivateFile()){
                uploadFileToS3BucketPrivate(folderFileName, file);
            }
            if(!request.isPrivateFile()){
                uploadFileToS3BucketPublic(folderFileName, file);
            }
            fileStorageResponse.setSuccess(true);
            fileStorageResponse.setFileId(folderFileName);
            fileStorageResponse.setUrl(fileUrl);
            fileStorageResponse.setFileSizeInKB(fileSize);
        } catch (IOException | AmazonServiceException e ) {
            e.printStackTrace();
        }
        if(file != null) {
            log.info("temp file {} deleted: {}", file.getName(), file.delete());
        }
        return fileStorageResponse;
    }

    @Override
    public void deleteResourceFile(String fileId) {
        s3Client.deleteObject(new DeleteObjectRequest(applicationProperty.getAmazonS3BucketName(), fileId));
    }

    private void uploadFileToS3BucketPrivate(String fileName, File file) {
        s3Client.putObject(new PutObjectRequest(applicationProperty.getAmazonS3BucketName(), fileName, file).
                withCannedAcl(CannedAccessControlList.Private));
    }

    private void uploadFileToS3BucketPublic(String fileName, File file) {
        s3Client.putObject(new PutObjectRequest(applicationProperty.getAmazonS3BucketName(), fileName, file).
                withCannedAcl(CannedAccessControlList.PublicRead));
    }

    @Override
    public String generateURLForPrivateFileAccess(String fileId){
        String preSignedURL = null;
        try{
            Date expiration = new Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += ((long) applicationProperty.getAmazonPrivateUrlExpirationTimeInMinutes() * 60 * 1000);
            expiration.setTime(expTimeMillis);

            GeneratePresignedUrlRequest preSignedUrlRequest = new GeneratePresignedUrlRequest(applicationProperty.getAmazonS3BucketName(), fileId)
                    .withMethod(HttpMethod.GET).withExpiration(expiration);

            preSignedURL = s3Client.generatePresignedUrl(preSignedUrlRequest).toString();
        }catch (AmazonServiceException e) {
            e.printStackTrace();
        }
        return  preSignedURL;
    }


    private File convertByteArrayToFile(byte[] data, String fileName) throws IOException {
        String tempFileName = String.format("temp_%s_%s", UUID.randomUUID().toString(), fileName.replace(" ", "_"));
        File file = new File(tempFileName);
        try(OutputStream os = new FileOutputStream(file)) {
            os.write(data);
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
        return file;
    }

    private String generateFileName(String fileName) {
        return UUID.randomUUID().toString() + new Date().getTime() +"_"+ fileName.replace(" ", "_");
    }


    public byte[] downloadFile(String fileKey) {
        byte[] content = null;
        S3Object s3Object = s3Client.getObject(applicationProperty.getAmazonS3BucketName(), fileKey);
        S3ObjectInputStream stream = s3Object.getObjectContent();
        try {
            content = IOUtils.toByteArray(stream);
            s3Object.close();
        } catch(final IOException ex) {
            System.out.println("IO Error Message= " + ex.getMessage());
        }
        return content;
    }
}
