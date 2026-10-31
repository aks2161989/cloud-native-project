package com.akshay.productcatalog.productcatalogapi.controller;

import com.akshay.productcatalog.productcatalogapi.dto.ImageResponse;
import com.akshay.productcatalog.productcatalogapi.service.ImageService;
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
        ImageResponse response = imageService.uploadImage(productId, authentication.getName(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ImageResponse>> getImages(
            @PathVariable Long productId
    ) {
        List<ImageResponse> responses = imageService.getImagesForProduct(productId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<ImageResponse> getImage(
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        ImageResponse response = imageService.getImage(productId, imageId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId,
            Authentication authentication
    ) {
        imageService.deleteImage(productId, imageId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
