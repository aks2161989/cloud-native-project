package com.akshay.productcatalog.productcatalogapi;

import com.akshay.productcatalog.productcatalogapi.entity.Product;
import com.akshay.productcatalog.productcatalogapi.entity.User;
import com.akshay.productcatalog.productcatalogapi.repository.ImageRepository;
import com.akshay.productcatalog.productcatalogapi.repository.ProductRepository;
import com.akshay.productcatalog.productcatalogapi.repository.UserRepository;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class ImageApiRestAssuredTest extends BaseRestAssuredTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private final List<Long> productIdsToDelete = new ArrayList<>();
    private final List<Long> userIdsToDelete = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        imageRepository.deleteAll();
        productRepository.deleteAllById(productIdsToDelete);
        userRepository.deleteAllById(userIdsToDelete);
        productIdsToDelete.clear();
        userIdsToDelete.clear();
    }

    private User createUser(String username, String password) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        User saved = userRepository.save(user);
        userIdsToDelete.add(saved.getId());
        return saved;
    }

    private Product createProduct(User owner) {
        Product product = new Product();
        product.setName("Test Product");
        product.setDescription("Test Description");
        product.setSku("SKU-" + UUID.randomUUID());
        product.setManufacturer("Manufacturer");
        product.setQuantity(10);
        product.setOwner(owner);
        Product saved = productRepository.save(product);
        productIdsToDelete.add(saved.getId());
        return saved;
    }

    @Test
    void ownerCanUploadImageAndRetrieveMetadata() {
        String username = TestCredentials.uniqueUsername("img-owner");
        String password = TestCredentials.PASSWORD;
        User owner = createUser(username, password);
        Product product = createProduct(owner);

        Response response = given()
                .auth().preemptive().basic(username, password)
                .multiPart("file", "test.png", "image-content".getBytes(), "image/png")
                .when()
                .post("/v1/product/{productId}/image", product.getId());

        response.then()
                .statusCode(201)
                .body("image_id", notNullValue())
                .body("product_id", equalTo(product.getId().intValue()))
                .body("file_name", equalTo("test.png"))
                .body("date_created", notNullValue())
                .body("s3_bucket_path", containsString("test-bucket"));

        Integer imageId = response.jsonPath().getInt("image_id");

        // Get single image metadata
        given()
                .when()
                .get("/v1/product/{productId}/image/{imageId}", product.getId(), imageId)
                .then()
                .statusCode(200)
                .body("image_id", equalTo(imageId))
                .body("file_name", equalTo("test.png"));

        // Get all images
        given()
                .when()
                .get("/v1/product/{productId}/image", product.getId())
                .then()
                .statusCode(200)
                .body("size()", equalTo(1));

        // Delete image by owner
        given()
                .auth().preemptive().basic(username, password)
                .when()
                .delete("/v1/product/{productId}/image/{imageId}", product.getId(), imageId)
                .then()
                .statusCode(204);

        // Verify image deleted
        given()
                .when()
                .get("/v1/product/{productId}/image/{imageId}", product.getId(), imageId)
                .then()
                .statusCode(404);
    }

    @Test
    void nonOwnerCannotUploadOrDeleteImage() {
        String ownerUsername = TestCredentials.uniqueUsername("product-owner");
        String nonOwnerUsername = TestCredentials.uniqueUsername("non-owner");
        String password = TestCredentials.PASSWORD;

        User owner = createUser(ownerUsername, password);
        User nonOwner = createUser(nonOwnerUsername, password);
        Product product = createProduct(owner);

        // Non-owner cannot upload
        given()
                .auth().preemptive().basic(nonOwnerUsername, password)
                .multiPart("file", "test.jpg", "content".getBytes(), "image/jpeg")
                .when()
                .post("/v1/product/{productId}/image", product.getId())
                .then()
                .statusCode(403);

        // Upload as owner first
        Response uploadResp = given()
                .auth().preemptive().basic(ownerUsername, password)
                .multiPart("file", "test.jpg", "content".getBytes(), "image/jpeg")
                .when()
                .post("/v1/product/{productId}/image", product.getId());

        Integer imageId = uploadResp.jsonPath().getInt("image_id");

        // Non-owner cannot delete
        given()
                .auth().preemptive().basic(nonOwnerUsername, password)
                .when()
                .delete("/v1/product/{productId}/image/{imageId}", product.getId(), imageId)
                .then()
                .statusCode(403);
    }

    @Test
    void invalidFileTypeReturns400() {
        String username = TestCredentials.uniqueUsername("invalid-file-user");
        String password = TestCredentials.PASSWORD;
        User owner = createUser(username, password);
        Product product = createProduct(owner);

        given()
                .auth().preemptive().basic(username, password)
                .multiPart("file", "document.pdf", "pdf-data".getBytes(), "application/pdf")
                .when()
                .post("/v1/product/{productId}/image", product.getId())
                .then()
                .statusCode(400);
    }

    @Test
    void unauthenticatedUploadReturns401() {
        String username = TestCredentials.uniqueUsername("unauth-user");
        User owner = createUser(username, TestCredentials.PASSWORD);
        Product product = createProduct(owner);

        given()
                .multiPart("file", "test.png", "data".getBytes(), "image/png")
                .when()
                .post("/v1/product/{productId}/image", product.getId())
                .then()
                .statusCode(401);
    }
}
