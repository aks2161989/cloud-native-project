package com.akshay.productcatalog.productcatalogapi.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "health_checks")
public class HealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "check_id")
    private long checkId;

    @Column(name = "check_datetime", nullable = false)
    private Instant checkDatetime;

    @PrePersist
    protected void onCreate(){
        this.checkDatetime = Instant.now();
    }

}
