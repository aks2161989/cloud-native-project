package com.akshay.productcatalog.productcatalogapi.dto;

import com.akshay.productcatalog.productcatalogapi.entity.Product;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private String sku;
    private String manufacturer;
    private Integer quantity;

    @JsonProperty("date_added")
    private OffsetDateTime dateAdded;

    @JsonProperty("date_last_updated")
    private OffsetDateTime dateLastUpdated;

    @JsonProperty("owner_user_id")
    private Long ownerUserId;

    public ProductResponse(Product product) {

        this.id = product.getId();
        this.name = product.getName();
        this.description = product.getDescription();
        this.sku = product.getSku();
        this.manufacturer = product.getManufacturer();
        this.quantity = product.getQuantity();
        this.dateAdded = product.getDateAdded();
        this.dateLastUpdated = product.getDateLastUpdated();
        this.ownerUserId = product.getOwner().getId();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSku() {
        return sku;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public OffsetDateTime getDateAdded() {
        return dateAdded;
    }

    public OffsetDateTime getDateLastUpdated() {
        return dateLastUpdated;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }
}
