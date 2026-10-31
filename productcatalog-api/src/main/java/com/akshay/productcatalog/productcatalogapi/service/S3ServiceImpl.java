package com.akshay.productcatalog.productcatalogapi.service;

import jakarta.annotation.PostConstruct;
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

    @Value("${aws.s3.bucket.name:}")
    private String bucketName;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    private S3Client s3Client;

    public S3ServiceImpl() {
    }

    public S3ServiceImpl(S3Client s3Client, String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @PostConstruct
    public void init() {
        if (s3Client == null && bucketName != null && !bucketName.isBlank()) {
            try {
                this.s3Client = S3Client.builder()
                        .region(Region.of(awsRegion))
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .build();
            } catch (Exception e) {
                // If running in local test environment without AWS credentials, s3Client remains uninitialized
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

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileData));
        return bucketName + "/" + key;
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

        s3Client.deleteObject(deleteObjectRequest);
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
}
