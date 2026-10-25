package com.akshay.productcatalog.productcatalogapi;

import java.util.UUID;

public final class TestCredentials {

    public static final String PASSWORD = "Password123";

    private TestCredentials() {
    }

    public static String uniqueUsername(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}