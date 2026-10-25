package com.akshay.productcatalog.productcatalogapi.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class UserUpdateRequest {

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;
    private String password;

    private final Map<String, Object> unSupportedFields = new HashMap<>();

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @JsonAnySetter
    public void addUnsupportedField(String fieldName, Object value){
        unSupportedFields.put(fieldName, value);
    }

    public boolean hasUnsupportedFields(){
        return !unSupportedFields.isEmpty();
    }
}
