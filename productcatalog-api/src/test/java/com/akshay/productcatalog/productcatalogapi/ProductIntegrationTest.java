package com.akshay.productcatalog.productcatalogapi;

import com.akshay.productcatalog.productcatalogapi.entity.Product;
import com.akshay.productcatalog.productcatalogapi.entity.User;
import com.akshay.productcatalog.productcatalogapi.repository.ProductRepository;
import com.akshay.productcatalog.productcatalogapi.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest
@AutoConfigureMockMvc
class ProductIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<Long> productIdsToDelete = new ArrayList<>();
    private final List<Long> userIdsToDelete = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        productRepository.deleteAllById(productIdsToDelete);
        userRepository.deleteAllById(userIdsToDelete);

        productIdsToDelete.clear();
        userIdsToDelete.clear();
    }

    private User createUser(String username, String rawPassword) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));

        User savedUser = userRepository.save(user);
        userIdsToDelete.add(savedUser.getId());

        return savedUser;
    }

    @Test
    void authenticatedUserCanCreateProduct() throws Exception {
        String username = "owner-" + UUID.randomUUID() + "@example.com";
        String password = "Password123";

        User owner = createUser(username, password);

        String requestBody = """
            {
              "name": "Keyboard",
              "description": "Mechanical keyboard",
              "sku": "KB-%s",
              "manufacturer": "KeyCorp",
              "quantity": 10
            }
            """.formatted(UUID.randomUUID());

        String responseBody = mockMvc.perform(post("/v1/product")
                        .with(httpBasic(username, password))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.owner_user_id").value(owner.getId()))
                .andExpect(jsonPath("$.date_added").exists())
                .andExpect(jsonPath("$.date_last_updated").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long productId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(responseBody)
                .get("id")
                .asLong();

        productIdsToDelete.add(productId);
    }

    @Test
    void nonOwnerCannotUpdateOrDeleteProduct() throws Exception {
        String ownerUsername =
                "owner-" + UUID.randomUUID() + "@example.com";
        String otherUsername =
                "other-" + UUID.randomUUID() + "@example.com";

        String ownerPassword = "OwnerPassword123";
        String otherPassword = "OtherPassword123";

        User owner = createUser(ownerUsername, ownerPassword);
        createUser(otherUsername, otherPassword);

        Product product = new Product();
        product.setName("Mouse");
        product.setDescription("Wireless mouse");
        product.setSku("MS-" + UUID.randomUUID());
        product.setManufacturer("MouseCorp");
        product.setQuantity(15);
        product.setOwner(owner);

        Product savedProduct = productRepository.save(product);
        productIdsToDelete.add(savedProduct.getId());

        mockMvc.perform(put("/v1/product/{productId}", savedProduct.getId())
                        .with(httpBasic(otherUsername, otherPassword))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "quantity": 50
                            }
                            """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/v1/product/{productId}", savedProduct.getId())
                        .with(httpBasic(otherUsername, otherPassword)))
                .andExpect(status().isForbidden());

        Product unchangedProduct = productRepository
                .findById(savedProduct.getId())
                .orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals(
                15,
                unchangedProduct.getQuantity()
        );
    }

    @Test
    void productCanBeRetrievedWithoutAuthentication() throws Exception {
        String username =
                "public-product-owner-" + UUID.randomUUID() + "@example.com";
        String password = "Password123";

        User owner = createUser(username, password);

        Product product = new Product();
        product.setName("Public Product");
        product.setDescription("Publicly retrievable product");
        product.setSku("PUBLIC-" + UUID.randomUUID());
        product.setManufacturer("PublicCorp");
        product.setQuantity(12);
        product.setOwner(owner);

        Product savedProduct = productRepository.save(product);
        productIdsToDelete.add(savedProduct.getId());

        mockMvc.perform(
                        get("/v1/product/{productId}", savedProduct.getId())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedProduct.getId()))
                .andExpect(jsonPath("$.name").value("Public Product"))
                .andExpect(jsonPath("$.owner_user_id").value(owner.getId()));
    }
}