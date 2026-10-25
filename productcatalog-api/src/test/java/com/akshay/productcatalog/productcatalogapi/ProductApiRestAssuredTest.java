package com.akshay.productcatalog.productcatalogapi;

import com.akshay.productcatalog.productcatalogapi.entity.Product;
import com.akshay.productcatalog.productcatalogapi.entity.User;
import com.akshay.productcatalog.productcatalogapi.repository.ProductRepository;
import com.akshay.productcatalog.productcatalogapi.repository.UserRepository;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.*;

class ProductApiRestAssuredTest extends BaseRestAssuredTest {

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

    @Test
    void authenticatedUserCanCreateProductAndPersistIt() {
        String username = TestCredentials.uniqueUsername("product-owner");
        String password = TestCredentials.PASSWORD;

        User owner = createUser(username, password);
        String sku = "KB-" + UUID.randomUUID();

        String requestBody = """
                {
                  "name": "Keyboard",
                  "description": "Mechanical keyboard",
                  "sku": "%s",
                  "manufacturer": "KeyCorp",
                  "quantity": 10
                }
                """.formatted(sku);

        Response response =
                given()
                        .auth()
                        .preemptive()
                        .basic(username, password)
                        .contentType(ContentType.JSON)
                        .body(requestBody)
                        .when()
                        .post("/v1/product");

        response.then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("name", equalTo("Keyboard"))
                .body("description", equalTo("Mechanical keyboard"))
                .body("sku", equalTo(sku))
                .body("manufacturer", equalTo("KeyCorp"))
                .body("quantity", equalTo(10))
                .body("owner_user_id", equalTo(owner.getId().intValue()))
                .body("$", hasKey("id"))
                .body("$", hasKey("date_added"))
                .body("$", hasKey("date_last_updated"));

        Long productId = response.jsonPath()
                .getLong("id");

        productIdsToDelete.add(productId);

        Product savedProduct = productRepository.findById(productId)
                .orElseThrow();

        assertEquals("Keyboard", savedProduct.getName());
        assertEquals("Mechanical keyboard", savedProduct.getDescription());
        assertEquals(sku, savedProduct.getSku());
        assertEquals("KeyCorp", savedProduct.getManufacturer());
        assertEquals(10, savedProduct.getQuantity());
        assertEquals(owner.getId(), savedProduct.getOwner().getId());
        assertNotNull(savedProduct.getDateAdded());
        assertNotNull(savedProduct.getDateLastUpdated());
    }

    @Test
    void productCanBeRetrievedWithoutAuthentication() {
        User owner = createUser(
                TestCredentials.uniqueUsername("public-product-owner"),
                TestCredentials.PASSWORD
        );

        Product savedProduct = createProduct(owner);

        given()
                .when()
                .get("/v1/product/{productId}", savedProduct.getId())
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(savedProduct.getId().intValue()))
                .body("name", equalTo(savedProduct.getName()))
                .body("description", equalTo(savedProduct.getDescription()))
                .body("sku", equalTo(savedProduct.getSku()))
                .body("manufacturer", equalTo(savedProduct.getManufacturer()))
                .body("quantity", equalTo(savedProduct.getQuantity()))
                .body("owner_user_id", equalTo(owner.getId().intValue()))
                .body("$", hasKey("date_added"))
                .body("$", hasKey("date_last_updated"));
    }

    @Test
    void gettingNonexistentProductReturns404() {
        given()
                .when()
                .get("/v1/product/{productId}", Long.MAX_VALUE)
                .then()
                .statusCode(404);
    }

    private User createUser(String username, String rawPassword) {
        User user = new User();
        user.setFirstName("Product");
        user.setLastName("Owner");
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));

        User savedUser = userRepository.save(user);
        userIdsToDelete.add(savedUser.getId());

        return savedUser;
    }

    private Product createProduct(User owner) {
        Product product = new Product();
        product.setName("Public Product");
        product.setDescription("Publicly retrievable product");
        product.setSku("PUBLIC-" + UUID.randomUUID());
        product.setManufacturer("PublicCorp");
        product.setQuantity(12);
        product.setOwner(owner);

        Product savedProduct = productRepository.save(product);
        productIdsToDelete.add(savedProduct.getId());

        return savedProduct;
    }

    @Test
    void ownerCanUpdateOwnProduct() {
        String username = TestCredentials.uniqueUsername("owner");
        String password = TestCredentials.PASSWORD;

        User owner = createUser(username, password);
        Product product = createProduct(owner);

        String updateBody = """
            {
              "name":"Updated Keyboard",
              "description":"Updated Description",
              "sku":"UPDATED-SKU",
              "manufacturer":"Updated Corp",
              "quantity":25
            }
            """;

        given()
                .auth()
                .preemptive()
                .basic(username, password)
                .contentType(ContentType.JSON)
                .body(updateBody)
                .when()
                .put("/v1/product/{id}", product.getId())
                .then()
                .statusCode(204);

        Product updated = productRepository.findById(product.getId()).orElseThrow();

        assertEquals("Updated Keyboard", updated.getName());
        assertEquals("Updated Description", updated.getDescription());
        assertEquals("UPDATED-SKU", updated.getSku());
        assertEquals("Updated Corp", updated.getManufacturer());
        assertEquals(25, updated.getQuantity());
    }

    @Test
    void nonOwnerCannotUpdateProduct() {

        User owner = createUser(
                TestCredentials.uniqueUsername("owner"),
                TestCredentials.PASSWORD);

        User attacker = createUser(
                TestCredentials.uniqueUsername("attacker"),
                TestCredentials.PASSWORD);

        Product product = createProduct(owner);

        String body = """
            {
              "name":"Hack"
            }
            """;

        given()
                .auth()
                .preemptive()
                .basic(attacker.getUsername(), TestCredentials.PASSWORD)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put("/v1/product/{id}", product.getId())
                .then()
                .statusCode(403);

        Product unchanged = productRepository.findById(product.getId()).orElseThrow();

        assertEquals("Public Product", unchanged.getName());
    }

    @Test
    void ownerCanDeleteOwnProduct() {

        String username = TestCredentials.uniqueUsername("delete-owner");

        User owner = createUser(username, TestCredentials.PASSWORD);

        Product product = createProduct(owner);

        given()
                .auth()
                .preemptive()
                .basic(username, TestCredentials.PASSWORD)
                .when()
                .delete("/v1/product/{id}", product.getId())
                .then()
                .statusCode(204);

        productIdsToDelete.remove(product.getId());

        assertFalse(productRepository.findById(product.getId()).isPresent());
    }

    @Test
    void nonOwnerCannotDeleteProduct() {

        User owner = createUser(
                TestCredentials.uniqueUsername("owner"),
                TestCredentials.PASSWORD);

        User attacker = createUser(
                TestCredentials.uniqueUsername("attacker"),
                TestCredentials.PASSWORD);

        Product product = createProduct(owner);

        given()
                .auth()
                .preemptive()
                .basic(attacker.getUsername(), TestCredentials.PASSWORD)
                .when()
                .delete("/v1/product/{id}", product.getId())
                .then()
                .statusCode(403);

        assertTrue(productRepository.findById(product.getId()).isPresent());
    }

    @Test
    void createProductWithoutAuthenticationReturns401() {

        String requestBody = """
            {
              "name":"Keyboard",
              "description":"Mechanical",
              "sku":"SKU123",
              "manufacturer":"KeyCorp",
              "quantity":10
            }
            """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/v1/product")
                .then()
                .statusCode(401);
    }

    @Test
    void createProductWithMissingNameReturns400() {

        String username = TestCredentials.uniqueUsername("validation");
        User owner = createUser(username, TestCredentials.PASSWORD);

        String requestBody = """
            {
              "description":"Mechanical",
              "sku":"SKU123",
              "manufacturer":"KeyCorp",
              "quantity":10
            }
            """;

        given()
                .auth()
                .preemptive()
                .basic(username, TestCredentials.PASSWORD)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/v1/product")
                .then()
                .statusCode(400);
    }

    @Test
    void createProductWithNegativeQuantityReturns400() {

        String username = TestCredentials.uniqueUsername("quantity");
        User owner = createUser(username, TestCredentials.PASSWORD);

        String requestBody = """
            {
              "name":"Keyboard",
              "description":"Mechanical",
              "sku":"SKU123",
              "manufacturer":"KeyCorp",
              "quantity":-1
            }
            """;

        given()
                .auth()
                .preemptive()
                .basic(username, TestCredentials.PASSWORD)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/v1/product")
                .then()
                .statusCode(400);
    }

    @Test
    void deletedProductReturns404() {

        String username = TestCredentials.uniqueUsername("delete-check");

        User owner = createUser(username, TestCredentials.PASSWORD);

        Product product = createProduct(owner);

        given()
                .auth()
                .preemptive()
                .basic(username, TestCredentials.PASSWORD)
                .when()
                .delete("/v1/product/{id}", product.getId())
                .then()
                .statusCode(204);

        productIdsToDelete.remove(product.getId());

        given()
                .when()
                .get("/v1/product/{id}", product.getId())
                .then()
                .statusCode(404);
    }
}