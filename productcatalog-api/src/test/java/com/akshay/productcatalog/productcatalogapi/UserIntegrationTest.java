package com.akshay.productcatalog.productcatalogapi;

import com.akshay.productcatalog.productcatalogapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createUserReturns201AndDoesNotExposePassword() throws Exception {
        String username = "user-" + UUID.randomUUID() + "@example.com";

        String requestBody = """
                {
                  "first_name": "Integration",
                  "last_name": "Test",
                  "username": "%s",
                  "password": "Password123"
                }
                """.formatted(username);

        mockMvc.perform(post("/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.first_name").value("Integration"))
                .andExpect(jsonPath("$.last_name").value("Test"))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.account_created").exists())
                .andExpect(jsonPath("$.account_updated").exists())
                .andExpect(jsonPath("$.password").doesNotExist());

        var savedUser = userRepository.findByUsername(username)
                .orElseThrow();

        // Verify that the database did not store the plaintext password.
        org.junit.jupiter.api.Assertions.assertNotEquals(
                "Password123",
                savedUser.getPassword()
        );

        org.junit.jupiter.api.Assertions.assertTrue(
                savedUser.getPassword().startsWith("$2")
        );

        userRepository.delete(savedUser);
    }

    @Test
    void creatingDuplicateUserReturns400() throws Exception {
        String username = "duplicate-" + UUID.randomUUID() + "@example.com";

        String requestBody = """
                {
                  "first_name": "Duplicate",
                  "last_name": "User",
                  "username": "%s",
                  "password": "Password123"
                }
                """.formatted(username);

        mockMvc.perform(post("/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        userRepository.findByUsername(username)
                .ifPresent(userRepository::delete);
    }

    @Test
    void creatingUserWithInvalidEmailReturns400() throws Exception {
        String requestBody = """
            {
              "first_name": "Invalid",
              "last_name": "Email",
              "username": "not-an-email",
              "password": "Password123"
            }
            """;

        mockMvc.perform(post("/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/v1/user/{userId}", 999))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserWithValidCredentialsReturnsUser() throws Exception {
        String username = "auth-" + UUID.randomUUID() + "@example.com";
        String password = "Password123";

        String requestBody = """
            {
              "first_name": "Authenticated",
              "last_name": "User",
              "username": "%s",
              "password": "%s"
            }
            """.formatted(username, password);

        mockMvc.perform(post("/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        var savedUser = userRepository.findByUsername(username)
                .orElseThrow();

        mockMvc.perform(get("/v1/user/{userId}", savedUser.getId())
                        .with(httpBasic(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.first_name").value("Authenticated"))
                .andExpect(jsonPath("$.last_name").value("User"))
                .andExpect(jsonPath("$.password").doesNotExist());

        userRepository.delete(savedUser);
    }

    @Test
    void userCannotAccessOrUpdateAnotherUser() throws Exception {
        String firstUsername =
                "first-" + UUID.randomUUID() + "@example.com";
        String secondUsername =
                "second-" + UUID.randomUUID() + "@example.com";

        String password = "Password123";

        String firstRequest = """
            {
              "first_name": "First",
              "last_name": "User",
              "username": "%s",
              "password": "%s"
            }
            """.formatted(firstUsername, password);

        String secondRequest = """
            {
              "first_name": "Second",
              "last_name": "User",
              "username": "%s",
              "password": "%s"
            }
            """.formatted(secondUsername, password);

        mockMvc.perform(post("/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequest))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondRequest))
                .andExpect(status().isCreated());

        var firstUser = userRepository.findByUsername(firstUsername)
                .orElseThrow();

        var secondUser = userRepository.findByUsername(secondUsername)
                .orElseThrow();

        mockMvc.perform(get("/v1/user/{userId}", secondUser.getId())
                        .with(httpBasic(firstUsername, password)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/v1/user/{userId}", secondUser.getId())
                        .with(httpBasic(firstUsername, password))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "first_name": "Unauthorized"
                            }
                            """))
                .andExpect(status().isForbidden());

        userRepository.delete(firstUser);
        userRepository.delete(secondUser);
    }
}