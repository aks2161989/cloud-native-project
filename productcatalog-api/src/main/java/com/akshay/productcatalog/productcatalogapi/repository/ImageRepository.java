package com.akshay.productcatalog.productcatalogapi.repository;

import com.akshay.productcatalog.productcatalogapi.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByProductId(Long productId);
    Optional<Image> findByImageIdAndProductId(Long imageId, Long productId);
    void deleteByProductId(Long productId);
}
