package com.akshay.productcatalog.productcatalogapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String description;

    @NotBlank
    @Column(nullable = false)
    private String sku;

    @NotBlank
    @Column(nullable = false)
    private String manufacturer;

    @Min(0)
    @Max(100)
    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "date_added", nullable = false, updatable = false)
    private OffsetDateTime dateAdded;

    @Column(name = "date_last_updated", nullable = false)
    private OffsetDateTime dateLastUpdated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @PrePersist
    protected void onCreate(){
        OffsetDateTime now = OffsetDateTime.now();
        this.dateAdded = now;
        this.dateLastUpdated = now;
    }

    @PreUpdate
    protected void onUpdate(){
        this.dateLastUpdated = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public OffsetDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(OffsetDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    public OffsetDateTime getDateLastUpdated() {
        return dateLastUpdated;
    }

    public void setDateLastUpdated(OffsetDateTime dateLastUpdated) {
        this.dateLastUpdated = dateLastUpdated;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
