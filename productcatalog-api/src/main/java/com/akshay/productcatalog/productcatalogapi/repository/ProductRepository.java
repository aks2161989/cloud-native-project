package com.akshay.productcatalog.productcatalogapi.repository;

import com.akshay.productcatalog.productcatalogapi.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
