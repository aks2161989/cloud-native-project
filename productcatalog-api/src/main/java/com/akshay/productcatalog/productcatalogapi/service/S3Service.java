package com.akshay.productcatalog.productcatalogapi.service;

import java.io.IOException;

public interface S3Service {
    String uploadFile(String key, byte[] fileData, String contentType) throws IOException;
    void deleteFile(String key);
    String getBucketName();
}
