package com.akshay.productcatalog.productcatalogapi;

import com.akshay.productcatalog.productcatalogapi.dto.UserCreateRequest;
import com.akshay.productcatalog.productcatalogapi.entity.User;
import com.akshay.productcatalog.productcatalogapi.repository.UserRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailVerificationApiTest extends BaseRestAssuredTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("GET /validateEmail - Successfully verify email with valid token")
    void testValidateEmail_Success() {
        String username = "verify-" + UUID.randomUUID() + "@example.com";
        UserCreateRequest request = new UserCreateRequest("Jane", "Doe", username, "SecretPassword123!");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v1/user")
                .then()
                .statusCode(201);

        User user = userRepository.findByUsername(username).orElseThrow();
        String token = user.getVerificationToken();

        given()
                .queryParam("email", username)
                .queryParam("token", token)
                .when()
                .get("/validateEmail")
                .then()
                .statusCode(200)
                .body("message", containsStringIgnoringCase("verified"));

        User verifiedUser = userRepository.findByUsername(username).orElseThrow();
        assertTrue(verifiedUser.isEmailVerified());
    }

    @Test
    @DisplayName("GET /validateEmail - Fail when token is invalid")
    void testValidateEmail_InvalidToken() {
        String username = "verify-invalid-" + UUID.randomUUID() + "@example.com";
        UserCreateRequest request = new UserCreateRequest("Jane", "Doe", username, "SecretPassword123!");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v1/user")
                .then()
                .statusCode(201);

        given()
                .queryParam("email", username)
                .queryParam("token", "invalid-token-123")
                .when()
                .get("/validateEmail")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("GET /validateEmail - Fail when token has expired")
    void testValidateEmail_ExpiredToken() {
        String username = "verify-expired-" + UUID.randomUUID() + "@example.com";
        UserCreateRequest request = new UserCreateRequest("Jane", "Doe", username, "SecretPassword123!");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v1/user")
                .then()
                .statusCode(201);

        User user = userRepository.findByUsername(username).orElseThrow();
        user.setTokenExpiry(OffsetDateTime.now().minusMinutes(5));
        userRepository.save(user);

        given()
                .queryParam("email", username)
                .queryParam("token", user.getVerificationToken())
                .when()
                .get("/validateEmail")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("GET /validateEmail - Fail when user does not exist")
    void testValidateEmail_UserNotFound() {
        given()
                .queryParam("email", "nonexistent-" + UUID.randomUUID() + "@example.com")
                .queryParam("token", UUID.randomUUID().toString())
                .when()
                .get("/validateEmail")
                .then()
                .statusCode(400);
    }
}
