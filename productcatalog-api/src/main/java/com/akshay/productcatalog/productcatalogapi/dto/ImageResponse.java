package com.akshay.productcatalog.productcatalogapi.dto;

import com.akshay.productcatalog.productcatalogapi.entity.Image;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public class ImageResponse {

    @JsonProperty("image_id")
    private Long imageId;

    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("date_created")
    private OffsetDateTime dateCreated;

    @JsonProperty("s3_bucket_path")
    private String s3BucketPath;

    public ImageResponse() {
    }

    public ImageResponse(Image image) {
        this.imageId = image.getImageId();
        this.productId = image.getProductId();
        this.fileName = image.getFileName();
        this.dateCreated = image.getDateCreated();
        this.s3BucketPath = image.getS3BucketPath();
    }

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public OffsetDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(OffsetDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getS3BucketPath() {
        return s3BucketPath;
    }

    public void setS3BucketPath(String s3BucketPath) {
        this.s3BucketPath = s3BucketPath;
    }
}
