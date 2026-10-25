package com.akshay.productcatalog.productcatalogapi.dto;

import com.akshay.productcatalog.productcatalogapi.entity.User;

import java.time.OffsetDateTime;

public class UserResponse {

    private Long id;
    private String first_name;
    private String last_name;
    private String username;
    private OffsetDateTime account_created;
    private OffsetDateTime account_updated;

    public UserResponse(User user) {
        this.id = user.getId();
        this.first_name = user.getFirstName();
        this.last_name = user.getLastName();
        this.username = user.getUsername();
        this.account_created = user.getAccountCreated();
        this.account_updated = user.getAccountUpdated();
    }

    public Long getId() {
        return id;
    }

    public String getFirst_name() {
        return first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public String getUsername() {
        return username;
    }

    public OffsetDateTime getAccount_created() {
        return account_created;
    }

    public OffsetDateTime getAccount_updated() {
        return account_updated;
    }

}
