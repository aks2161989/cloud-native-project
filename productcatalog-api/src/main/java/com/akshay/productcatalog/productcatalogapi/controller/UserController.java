package com.akshay.productcatalog.productcatalogapi.controller;

import com.akshay.productcatalog.productcatalogapi.dto.UserUpdateRequest;
import com.akshay.productcatalog.productcatalogapi.dto.UserCreateRequest;
import com.akshay.productcatalog.productcatalogapi.dto.UserResponse;
import com.akshay.productcatalog.productcatalogapi.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.Authenticator;

@RestController
@RequestMapping("/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request
            ){

        UserResponse response = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable Long userId,
            Authentication authentication
    ){
        UserResponse response = userService.getuser(
                userId,
                authentication.getName()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<Void> updateUser(
            @PathVariable Long userId,
            Authentication authentication,
            @Valid @RequestBody UserUpdateRequest request
    ){
        userService.updateUser(
                userId,
                authentication.getName(),
                request
        );
        return ResponseEntity.noContent().build();
    }
}
