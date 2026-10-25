package com.akshay.productcatalog.productcatalogapi;

import com.akshay.productcatalog.productcatalogapi.repository.UserRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.hasKey;

class AuthenticationRestAssuredTest extends BaseRestAssuredTest {

    @Autowired
    private UserRepository userRepository;

    private String createdUsername;

    @AfterEach
    void cleanUp() {
        if (createdUsername != null) {
            userRepository.findByUsername(createdUsername)
                    .ifPresent(userRepository::delete);
        }
    }

    @Test
    void protectedEndpointWithoutCredentialsReturns401() {
        given()
                .when()
                .get("/v1/user/999999")
                .then()
                .statusCode(401);
    }

    @Test
    void protectedEndpointWithUnknownUsernameReturns401() {
        given()
                .auth()
                .preemptive()
                .basic(
                        TestCredentials.uniqueUsername("unknown"),
                        TestCredentials.PASSWORD
                )
                .when()
                .get("/v1/user/999999")
                .then()
                .statusCode(401);
    }

    @Test
    void protectedEndpointWithWrongPasswordReturns401() {
        createUser();

        long userId = userRepository.findByUsername(createdUsername)
                .orElseThrow()
                .getId();

        given()
                .auth()
                .preemptive()
                .basic(createdUsername, "WrongPassword123")
                .when()
                .get("/v1/user/{userId}", userId)
                .then()
                .statusCode(401);
    }

    @Test
    void protectedEndpointWithValidCredentialsReturns200() {
        createUser();

        long userId = userRepository.findByUsername(createdUsername)
                .orElseThrow()
                .getId();

        given()
                .auth()
                .preemptive()
                .basic(createdUsername, TestCredentials.PASSWORD)
                .when()
                .get("/v1/user/{userId}", userId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo((int) userId))
                .body("username", equalTo(createdUsername))
                .body("$", not(hasKey("password")));
    }

    private void createUser() {
        createdUsername = TestCredentials.uniqueUsername("auth");

        String requestBody = """
                {
                  "first_name": "Authentication",
                  "last_name": "Test",
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(
                createdUsername,
                TestCredentials.PASSWORD
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/v1/user")
                .then()
                .statusCode(201);
    }
}