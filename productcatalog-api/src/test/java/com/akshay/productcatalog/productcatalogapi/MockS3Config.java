package com.akshay.productcatalog.productcatalogapi;

import com.akshay.productcatalog.productcatalogapi.service.S3Service;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

@TestConfiguration
public class MockS3Config {

    @Bean
    @Primary
    public S3Service s3Service() {
        return new S3Service() {
            @Override
            public String uploadFile(String key, byte[] fileData, String contentType) throws IOException {
                return "test-bucket/" + key;
            }

            @Override
            public void deleteFile(String key) {
                // mock delete
            }

            @Override
            public String getBucketName() {
                return "test-bucket";
            }
        };
    }
}
