package com.akshay.productcatalog.productcatalogapi.service;

import com.akshay.productcatalog.productcatalogapi.entity.User;

public interface SnsService {
    void publishUserSignup(User user, String token);
}
