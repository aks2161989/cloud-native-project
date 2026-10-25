package com.akshay.productcatalog.productcatalogapi.service;

import com.akshay.productcatalog.productcatalogapi.dto.ProductRequest;
import com.akshay.productcatalog.productcatalogapi.dto.ProductResponse;
import com.akshay.productcatalog.productcatalogapi.entity.Product;
import com.akshay.productcatalog.productcatalogapi.entity.User;
import com.akshay.productcatalog.productcatalogapi.exception.ResourceNotFoundException;
import com.akshay.productcatalog.productcatalogapi.repository.ProductRepository;
import com.akshay.productcatalog.productcatalogapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.akshay.productcatalog.productcatalogapi.dto.ProductUpdateRequest;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductService(ProductRepository productRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public ProductResponse createProduct(
            String username,
            ProductRequest request
    ){
        if(request.hasUnsupportedFields()){
            throw new IllegalArgumentException(
                    "Unsupported fields supplied."
            );
        }
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found.")
                );

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setSku(request.getSku());
        product.setManufacturer(request.getManufacturer());
        product.setQuantity(request.getQuantity());

        product.setOwner(owner);

        Product savedProduct = productRepository.save(product);

        return new ProductResponse(savedProduct);
    }

    public ProductResponse getProduct(Long productId){
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found.")
                );

        return new ProductResponse(product);
    }

    @Transactional
    public void updateProduct(
            Long productId,
            String username,
            ProductUpdateRequest request
    ){
        if(request.hasUnsupportedFields()){
            throw new IllegalArgumentException(
                    "Only product fields may be updated."
            );
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found.")
                );

        if (!product.getOwner().getUsername().equals(username)){
            throw new SecurityException(
                    "Only the product owner may update this product."
            );
        }

        boolean hasUpdate = false;

        if(request.getName() != null){
            if(request.getName().isBlank()){
                throw new IllegalArgumentException(
                        "Product name cannot be blank."
                );
            }

            product.setName(request.getName());
            hasUpdate = true;
        }

        if(request.getDescription() != null){
            if (request.getDescription().isBlank()){
                throw new IllegalArgumentException(
                        "Description canot be blank."
                );
            }

            product.setDescription(request.getDescription());
            hasUpdate = true;
        }

        if(request.getSku() != null){
            if(request.getSku().isBlank()){
                throw new IllegalArgumentException(
                        "SKU cannot be blank"
                );
            }

            product.setSku(request.getSku());
            hasUpdate = true;
        }

        if(request.getManufacturer() != null){
            if (request.getManufacturer().isBlank()){
                throw new IllegalArgumentException(
                        "Manufacturer cannot be blank."
                );
            }

            product.setManufacturer(request.getManufacturer());
            hasUpdate = true;
        }

        if (request.getQuantity() != null){

            product.setQuantity(request.getQuantity());
            hasUpdate = true;
        }

        if(!hasUpdate){
            throw new IllegalArgumentException(
                    "At least one product field must be provided."
            );
        }

        productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(
            Long productId,
            String username
    ){
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product not found.")
                );

        if(!product.getOwner().getUsername().equals(username)){
            throw new SecurityException(
                    "Only the product owner may delete this product."
            );
        }

        productRepository.delete(product);
    }
}
