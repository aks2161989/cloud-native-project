package com.akshay.productcatalog.productcatalogapi.controller;

import com.akshay.productcatalog.productcatalogapi.dto.ProductRequest;
import com.akshay.productcatalog.productcatalogapi.dto.ProductResponse;
import com.akshay.productcatalog.productcatalogapi.dto.ProductUpdateRequest;
import com.akshay.productcatalog.productcatalogapi.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/product")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            Authentication authentication,
            @Valid @RequestBody ProductRequest request
    ) {
        logger.info("Request received to create product: name={}, sku={}, owner={}",
                request.getName(), request.getSku(), authentication.getName());
        ProductResponse response = productService.createProduct(
                authentication.getName(), request
        );
        logger.info("Product created successfully: id={}, sku={}", response.getId(), response.getSku());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable Long productId
    ) {
        logger.info("Request received to fetch product: productId={}", productId);
        ProductResponse response = productService.getProduct(productId);
        logger.info("Product retrieved successfully: productId={}", productId);

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/{productId}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<Void> updateProduct(
            @PathVariable Long productId,
            Authentication authentication,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        logger.info("Request received to update product: productId={}, user={}", productId, authentication.getName());
        productService.updateProduct(
                productId,
                authentication.getName(),
                request
        );
        logger.info("Product updated successfully: productId={}", productId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long productId,
            Authentication authentication
    ) {
        logger.info("Request received to delete product: productId={}, user={}", productId, authentication.getName());
        productService.deleteProduct(
                productId,
                authentication.getName()
        );
        logger.info("Product deleted successfully: productId={}", productId);

        return ResponseEntity.noContent().build();
    }
}
