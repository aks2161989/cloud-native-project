package com.akshay.productcatalog.productcatalogapi.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Service
public class S3ServiceImpl implements S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3ServiceImpl.class);

    @Value("${aws.s3.bucket.name:}")
    private String bucketName;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    private S3Client s3Client;

    @Autowired(required = false)
    private MetricsService metricsService;

    public S3ServiceImpl() {
    }

    public S3ServiceImpl(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public S3ServiceImpl(S3Client s3Client, String bucketName, MetricsService metricsService) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.metricsService = metricsService;
    }

    @PostConstruct
    public void init() {
        if (s3Client == null && bucketName != null && !bucketName.isBlank()) {
            try {
                this.s3Client = S3Client.builder()
                        .region(Region.of(awsRegion))
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .build();
                logger.info("S3Client initialized successfully for region {} and bucket {}", awsRegion, bucketName);
            } catch (Exception e) {
                logger.warn("Could not initialize S3Client in current environment: {}", e.getMessage());
            }
        }
    }

    @Override
    public String uploadFile(String key, byte[] fileData, String contentType) throws IOException {
        if (s3Client == null) {
            init();
        }
        if (s3Client == null) {
            throw new IllegalStateException("S3 client is not configured");
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        logger.info("Uploading file to S3: bucket={}, key={}, size={} bytes, contentType={}",
                bucketName, key, fileData.length, contentType);

        long startTime = System.currentTimeMillis();
        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileData));
            long duration = System.currentTimeMillis() - startTime;

            if (metricsService != null) {
                metricsService.recordS3CallTime("putObject", duration);
            }
            logger.info("Successfully uploaded file to S3: bucket={}, key={}, duration={}ms",
                    bucketName, key, duration);
            return bucketName + "/" + key;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            if (metricsService != null) {
                metricsService.recordS3CallTime("putObject", duration);
            }
            logger.error("Failed to upload file to S3: bucket={}, key={}, duration={}ms, error={}",
                    bucketName, key, duration, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void deleteFile(String key) {
        if (s3Client == null) {
            init();
        }
        if (s3Client == null) {
            throw new IllegalStateException("S3 client is not configured");
        }

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        logger.info("Deleting file from S3: bucket={}, key={}", bucketName, key);

        long startTime = System.currentTimeMillis();
        try {
            s3Client.deleteObject(deleteObjectRequest);
            long duration = System.currentTimeMillis() - startTime;

            if (metricsService != null) {
                metricsService.recordS3CallTime("deleteObject", duration);
            }
            logger.info("Successfully deleted file from S3: bucket={}, key={}, duration={}ms",
                    bucketName, key, duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            if (metricsService != null) {
                metricsService.recordS3CallTime("deleteObject", duration);
            }
            logger.error("Failed to delete file from S3: bucket={}, key={}, duration={}ms, error={}",
                    bucketName, key, duration, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    public void setS3Client(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public void setMetricsService(MetricsService metricsService) {
        this.metricsService = metricsService;
    }
}
