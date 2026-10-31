package com.akshay.productcatalog.productcatalogapi.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "images")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "date_created", nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @Column(name = "s3_bucket_path", nullable = false)
    private String s3BucketPath;

    public Image() {
    }

    public Image(Long productId, String fileName, String s3BucketPath) {
        this.productId = productId;
        this.fileName = fileName;
        this.s3BucketPath = s3BucketPath;
        this.dateCreated = OffsetDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.dateCreated == null) {
            this.dateCreated = OffsetDateTime.now();
        }
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
