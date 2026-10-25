package com.akshay.productcatalog.productcatalogapi;

import com.akshay.productcatalog.productcatalogapi.entity.User;
import com.akshay.productcatalog.productcatalogapi.repository.UserRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

class UserApiRestAssuredTest extends BaseRestAssuredTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String createdUsername;

    @AfterEach
    void cleanUp() {
        if (createdUsername != null) {
            userRepository.findByUsername(createdUsername)
                    .ifPresent(userRepository::delete);
        }
    }

    @Test
    void createUserReturns201AndPersistsSecureUser() {
        createdUsername = TestCredentials.uniqueUsername("create-user");

        String requestBody = """
                {
                  "first_name": "Rest",
                  "last_name": "Assured",
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(
                createdUsername,
                TestCredentials.PASSWORD
        );

        Response response =
                given()
                        .contentType(ContentType.JSON)
                        .body(requestBody)
                        .when()
                        .post("/v1/user");

        response.then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("first_name", equalTo("Rest"))
                .body("last_name", equalTo("Assured"))
                .body("username", equalTo(createdUsername))
                .body("$", not(hasKey("password")))
                .body("$", hasKey("id"))
                .body("$", hasKey("account_created"))
                .body("$", hasKey("account_updated"));

        User savedUser = userRepository.findByUsername(createdUsername)
                .orElseThrow();

        assertNotNull(savedUser.getId());
        assertNotNull(savedUser.getAccountCreated());
        assertNotNull(savedUser.getAccountUpdated());
        assertNotEquals(TestCredentials.PASSWORD, savedUser.getPassword());
        assertTrue(passwordEncoder.matches(
                TestCredentials.PASSWORD,
                savedUser.getPassword()
        ));
    }

    @Test
    void createDuplicateUserReturns400AndDoesNotCreateSecondRecord() {
        createdUsername = TestCredentials.uniqueUsername("duplicate-user");

        String requestBody = """
                {
                  "first_name": "Duplicate",
                  "last_name": "User",
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

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/v1/user")
                .then()
                .statusCode(400);

        assertEquals(
                1,
                userRepository.findAll()
                        .stream()
                        .filter(user ->
                                user.getUsername().equals(createdUsername)
                        )
                        .count()
        );
    }

    @Test
    void createUserWithInvalidEmailReturns400AndDoesNotPersistUser() {
        String invalidUsername = "not-an-email";

        String requestBody = """
                {
                  "first_name": "Invalid",
                  "last_name": "Email",
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(
                invalidUsername,
                TestCredentials.PASSWORD
        );

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/v1/user")
                .then()
                .statusCode(400);

        assertTrue(
                userRepository.findByUsername(invalidUsername).isEmpty()
        );
    }

    @Test
    void updateOwnFirstNameReturns204AndPreservesOtherFields() {
        createdUsername = TestCredentials.uniqueUsername("update-user");

        createUser(
                createdUsername,
                "Original",
                "Name",
                TestCredentials.PASSWORD
        );

        User beforeUpdate = userRepository.findByUsername(createdUsername)
                .orElseThrow();

        Long userId = beforeUpdate.getId();
        String originalLastName = beforeUpdate.getLastName();
        String originalPasswordHash = beforeUpdate.getPassword();
        var originalCreatedTimestamp = beforeUpdate.getAccountCreated();
        var originalUpdatedTimestamp = beforeUpdate.getAccountUpdated();

        String updateBody = """
                {
                  "first_name": "Updated"
                }
                """;

        given()
                .auth()
                .preemptive()
                .basic(
                        createdUsername,
                        TestCredentials.PASSWORD
                )
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/v1/user/{userId}", userId)
                .then()
                .statusCode(204);

        User updatedUser = userRepository.findById(userId)
                .orElseThrow();

        assertEquals("Updated", updatedUser.getFirstName());
        assertEquals(originalLastName, updatedUser.getLastName());
        assertEquals(createdUsername, updatedUser.getUsername());
        assertEquals(originalPasswordHash, updatedUser.getPassword());
        assertEquals(
                originalCreatedTimestamp,
                updatedUser.getAccountCreated()
        );
        assertTrue(
                updatedUser.getAccountUpdated()
                        .isAfter(originalUpdatedTimestamp)
                        || updatedUser.getAccountUpdated()
                        .isEqual(originalUpdatedTimestamp)
        );
    }

    @Test
    void updateUserWithUnsupportedUsernameFieldReturns400() {
        createdUsername = TestCredentials.uniqueUsername("invalid-update");

        createUser(
                createdUsername,
                "Original",
                "User",
                TestCredentials.PASSWORD
        );

        Long userId = userRepository.findByUsername(createdUsername)
                .orElseThrow()
                .getId();

        String updateBody = """
                {
                  "username": "changed@example.com"
                }
                """;

        given()
                .auth()
                .preemptive()
                .basic(
                        createdUsername,
                        TestCredentials.PASSWORD
                )
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/v1/user/{userId}", userId)
                .then()
                .statusCode(400);

        User unchangedUser = userRepository.findById(userId)
                .orElseThrow();

        assertEquals(createdUsername, unchangedUser.getUsername());
    }

    private void createUser(
            String username,
            String firstName,
            String lastName,
            String password
    ) {
        String requestBody = """
                {
                  "first_name": "%s",
                  "last_name": "%s",
                  "username": "%s",
                  "password": "%s"
                }
                """.formatted(
                firstName,
                lastName,
                username,
                password
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