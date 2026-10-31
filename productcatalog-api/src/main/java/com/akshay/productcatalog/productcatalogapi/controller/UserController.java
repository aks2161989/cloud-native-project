package com.akshay.productcatalog.productcatalogapi.controller;

import com.akshay.productcatalog.productcatalogapi.dto.UserCreateRequest;
import com.akshay.productcatalog.productcatalogapi.dto.UserResponse;
import com.akshay.productcatalog.productcatalogapi.dto.UserUpdateRequest;
import com.akshay.productcatalog.productcatalogapi.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/user")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request
    ) {
        logger.info("Request received to create user: username={}", request.getUsername());
        UserResponse response = userService.createUser(request);
        logger.info("User created successfully: id={}, username={}", response.getId(), response.getUsername());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable Long userId,
            Authentication authentication
    ) {
        logger.info("Request received to fetch user: userId={}, authenticatedUser={}", userId, authentication.getName());
        UserResponse response = userService.getuser(
                userId,
                authentication.getName()
        );
        logger.info("User retrieved successfully: userId={}", userId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<Void> updateUser(
            @PathVariable Long userId,
            Authentication authentication,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        logger.info("Request received to update user: userId={}, authenticatedUser={}", userId, authentication.getName());
        userService.updateUser(
                userId,
                authentication.getName(),
                request
        );
        logger.info("User updated successfully: userId={}", userId);

        return ResponseEntity.noContent().build();
    }
}
