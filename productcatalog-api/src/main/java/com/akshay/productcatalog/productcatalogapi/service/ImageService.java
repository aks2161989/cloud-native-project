package com.akshay.productcatalog.productcatalogapi.service;

import com.akshay.productcatalog.productcatalogapi.dto.ImageResponse;
import com.akshay.productcatalog.productcatalogapi.entity.Image;
import com.akshay.productcatalog.productcatalogapi.entity.Product;
import com.akshay.productcatalog.productcatalogapi.exception.ResourceNotFoundException;
import com.akshay.productcatalog.productcatalogapi.repository.ImageRepository;
import com.akshay.productcatalog.productcatalogapi.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png"
    );

    private final ImageRepository imageRepository;
    private final ProductRepository productRepository;
    private final S3Service s3Service;

    public ImageService(ImageRepository imageRepository, ProductRepository productRepository, S3Service s3Service) {
        this.imageRepository = imageRepository;
        this.productRepository = productRepository;
        this.s3Service = s3Service;
    }

    @Transactional
    public ImageResponse uploadImage(Long productId, String authUsername, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = getFileExtension(originalFilename);
        String contentType = file.getContentType();

        boolean validExtension = extension != null && ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
        boolean validContentType = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase());

        if (!validExtension && !validContentType) {
            throw new IllegalArgumentException("Only JPEG, JPG, and PNG image files are allowed");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getOwner().getUsername().equals(authUsername)) {
            throw new SecurityException("Only the product owner can upload images for this product");
        }

        // Partition user's images: ownerId/productId/UUID-filename
        Long ownerId = product.getOwner().getId();
        String uniqueFileName = UUID.randomUUID() + "-" + originalFilename;
        String s3Key = ownerId + "/" + productId + "/" + uniqueFileName;

        String s3BucketPath = s3Service.uploadFile(s3Key, file.getBytes(), contentType != null ? contentType : "image/" + extension);

        Image image = new Image(productId, originalFilename, s3BucketPath);
        Image saved = imageRepository.save(image);

        return new ImageResponse(saved);
    }

    public List<ImageResponse> getImagesForProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        return imageRepository.findByProductId(productId)
                .stream()
                .map(ImageResponse::new)
                .toList();
    }

    public ImageResponse getImage(Long productId, Long imageId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }

        Image image = imageRepository.findByImageIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        return new ImageResponse(image);
    }

    @Transactional
    public void deleteImage(Long productId, Long imageId, String authUsername) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getOwner().getUsername().equals(authUsername)) {
            throw new SecurityException("Only the product owner can delete images for this product");
        }

        Image image = imageRepository.findByImageIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        String path = image.getS3BucketPath();
        String bucket = s3Service.getBucketName();
        String s3Key = (bucket != null && !bucket.isEmpty() && path.startsWith(bucket + "/"))
                ? path.substring(bucket.length() + 1) : path;

        s3Service.deleteFile(s3Key);
        imageRepository.delete(image);
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return null;
    }
}
