package com.uni.research.common.service;

import com.uni.research.common.config.MinioConfig;
import com.uni.research.common.exception.BizException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import jakarta.annotation.PostConstruct;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(minioConfig.getBucketName())) {
            log.warn("MinIO bucket name is not configured!");
            return;
        }
        try {
            boolean found = minioClient
                    .bucketExists(BucketExistsArgs.builder().bucket(minioConfig.getBucketName()).build());
            if (!found) {
                log.info("Creating bucket: {}", minioConfig.getBucketName());
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioConfig.getBucketName()).build());
            } else {
                log.info("Bucket already exists: {}", minioConfig.getBucketName());
            }
        } catch (Exception e) {
            log.warn("Failed to check/create bucket: {}", e.getMessage());
        }
    }

    /**
     * Upload file to MinIO
     *
     * @param file     File from request
     * @param fileName Target filename in MinIO
     * @return File path in MinIO
     */
    public String uploadFile(MultipartFile file, String fileName) {
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();

            minioClient.putObject(args);
            return fileName;
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new BizException("File upload failed: " + e.getMessage());
        }
    }

    /**
     * Get file stream from MinIO
     */
    public InputStream getFile(String fileName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .build());
        } catch (Exception e) {
            log.error("Failed to get file from MinIO", e);
            throw new BizException("File download failed: " + e.getMessage());
        }
    }

    /**
     * Remove file from MinIO
     */
    public void removeFile(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .build());
        } catch (Exception e) {
            log.error("Failed to remove file from MinIO", e);
            throw new BizException("File deletion failed: " + e.getMessage());
        }
    }
}
