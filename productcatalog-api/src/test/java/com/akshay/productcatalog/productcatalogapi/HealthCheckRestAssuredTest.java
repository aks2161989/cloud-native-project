package com.akshay.productcatalog.productcatalogapi;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthCheckRestAssuredTest extends BaseRestAssuredTest {

    @Test
    void healthCheckReturns200WithExpectedHeadersAndNoBody() {
        Response response = RestAssured.get("/healthz");

        assertEquals(200, response.statusCode());
        assertEquals("no-cache", response.header("Cache-Control"));
        assertTrue(response.body().asString().isBlank());
    }
}