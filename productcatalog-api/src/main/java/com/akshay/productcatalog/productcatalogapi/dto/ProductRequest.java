package com.akshay.productcatalog.productcatalogapi.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.HashMap;
import java.util.Map;

public class ProductRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotBlank
    private String sku;

    @NotBlank
    private String manufacturer;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer quantity;

    private final Map<String, Object> unsupportedFields = new HashMap();

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

    @JsonAnySetter
    public void addUnsupportedField(String fieldName, Object value){
        unsupportedFields.put(fieldName, value);
    }

    public boolean hasUnsupportedFields(){
        return !unsupportedFields.isEmpty();
    }
}
