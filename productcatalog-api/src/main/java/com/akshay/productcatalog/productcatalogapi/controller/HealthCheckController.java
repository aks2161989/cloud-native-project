package com.akshay.productcatalog.productcatalogapi.controller;

import com.akshay.productcatalog.productcatalogapi.entity.HealthCheck;
import com.akshay.productcatalog.productcatalogapi.repository.HealthCheckRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);

    private final HealthCheckRepository healthCheckRepository;

    public HealthCheckController(HealthCheckRepository healthCheckRepository) {
        this.healthCheckRepository = healthCheckRepository;
    }

    @GetMapping("/healthz")
    public ResponseEntity<Void> healthz(@RequestBody(required = false) String body) {
        logger.info("Health check endpoint /healthz requested");

        if (body != null && !body.isBlank()) {
            logger.warn("Health check request contains unexpected payload body");
            return ResponseEntity.badRequest()
                    .cacheControl(CacheControl.noCache())
                    .build();
        }

        try {
            healthCheckRepository.save(new HealthCheck());
            logger.info("Health check succeeded: database connection is healthy");
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .build();
        } catch (Exception e) {
            logger.error("Health check failed: unable to connect to database", e);
            return ResponseEntity.status(503)
                    .cacheControl(CacheControl.noCache())
                    .build();
        }
    }
}
