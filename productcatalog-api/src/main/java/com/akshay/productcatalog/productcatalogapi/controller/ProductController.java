package com.akshay.productcatalog.productcatalogapi.controller;

import com.akshay.productcatalog.productcatalogapi.dto.ProductRequest;
import com.akshay.productcatalog.productcatalogapi.dto.ProductResponse;
import com.akshay.productcatalog.productcatalogapi.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.akshay.productcatalog.productcatalogapi.dto.ProductUpdateRequest;

@RestController
@RequestMapping("/v1/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            Authentication authentication,
            @Valid @RequestBody ProductRequest request
            ){
        ProductResponse response = productService.createProduct(
                authentication.getName(), request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable Long productId
    ){
        ProductResponse response = productService.getProduct(productId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Void> updateProduct(
            @PathVariable Long productId,
            Authentication authentication,
            @Valid @RequestBody ProductUpdateRequest request
    ){
        productService.updateProduct(
                productId,
                authentication.getName(),
                request
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long productId,
            Authentication authentication
    ){
        productService.deleteProduct(
                productId,
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}
