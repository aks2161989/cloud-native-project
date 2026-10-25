package com.akshay.productcatalog.productcatalogapi.controller;

import com.akshay.productcatalog.productcatalogapi.entity.HealthCheck;
import com.akshay.productcatalog.productcatalogapi.repository.HealthCheckRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class HealthCheckController {

    private final HealthCheckRepository healthCheckRepository;

    public HealthCheckController(HealthCheckRepository healthCheckRepository){
        this.healthCheckRepository = healthCheckRepository;
    }

    @GetMapping("/healthz")
    public ResponseEntity<Void> healthz(@RequestBody(required = false) String body){
        if(body != null && !body.isBlank()){
            return ResponseEntity.badRequest().
                    cacheControl(CacheControl.noCache()).build();
        }

        try{
            healthCheckRepository.save(new HealthCheck());

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache()).build();
        }catch (Exception e){
            return ResponseEntity.status(503)
                    .cacheControl(CacheControl.noCache())
                    .build();
        }
    }
}
