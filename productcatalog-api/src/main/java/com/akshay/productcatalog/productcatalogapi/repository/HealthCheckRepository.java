package com.akshay.productcatalog.productcatalogapi.repository;

import com.akshay.productcatalog.productcatalogapi.entity.HealthCheck;
import org.springframework.data.jpa.repository.JpaRepository;
public interface HealthCheckRepository extends JpaRepository<HealthCheck, Long>{
}
