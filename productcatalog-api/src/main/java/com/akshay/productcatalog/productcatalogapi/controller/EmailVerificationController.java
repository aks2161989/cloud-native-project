package com.akshay.productcatalog.productcatalogapi.controller;

import com.akshay.productcatalog.productcatalogapi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class EmailVerificationController {

    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationController.class);
    private final UserService userService;

    public EmailVerificationController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/validateEmail")
    public ResponseEntity<Map<String, String>> validateEmail(
            @RequestParam("email") String email,
            @RequestParam("token") String token) {
        logger.info("Email verification requested for user: {}", email);
        userService.verifyEmail(email, token);
        logger.info("Email verification successful for user: {}", email);
        return ResponseEntity.ok(Map.of("message", "User email verified successfully."));
    }
}
