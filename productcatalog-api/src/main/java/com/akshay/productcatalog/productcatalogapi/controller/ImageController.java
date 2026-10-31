package com.akshay.productcatalog.productcatalogapi.controller;

import com.akshay.productcatalog.productcatalogapi.dto.ImageResponse;
import com.akshay.productcatalog.productcatalogapi.service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1/product/{productId}/image")
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping
    public ResponseEntity<ImageResponse> uploadImage(
            @PathVariable Long productId,
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        logger.info("Request received to upload image: productId={}, filename={}, user={}",
                productId, file.getOriginalFilename(), authentication.getName());
        ImageResponse response = imageService.uploadImage(productId, authentication.getName(), file);
        logger.info("Image uploaded successfully: imageId={}, productId={}, s3Path={}",
                response.getImageId(), productId, response.getS3BucketPath());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ImageResponse>> getImages(
            @PathVariable Long productId
    ) {
        logger.info("Request received to list images: productId={}", productId);
        List<ImageResponse> responses = imageService.getImagesForProduct(productId);
        logger.info("Retrieved {} images for productId={}", responses.size(), productId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<ImageResponse> getImage(
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        logger.info("Request received to get image: productId={}, imageId={}", productId, imageId);
        ImageResponse response = imageService.getImage(productId, imageId);
        logger.info("Image retrieved successfully: imageId={}, productId={}", imageId, productId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            Authentication authentication
    ) {
        logger.info("Request received to delete image: productId={}, imageId={}, user={}",
                productId, imageId, authentication.getName());
        imageService.deleteImage(productId, imageId, authentication.getName());
        logger.info("Image deleted successfully: imageId={}, productId={}", imageId, productId);
        return ResponseEntity.noContent().build();
    }
}
