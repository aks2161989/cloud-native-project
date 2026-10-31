package com.akshay.productcatalog.productcatalogapi.service;

import com.akshay.productcatalog.productcatalogapi.dto.UserCreateRequest;
import com.akshay.productcatalog.productcatalogapi.dto.UserResponse;
import com.akshay.productcatalog.productcatalogapi.dto.UserUpdateRequest;
import com.akshay.productcatalog.productcatalogapi.entity.User;
import com.akshay.productcatalog.productcatalogapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SnsService snsService;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, SnsService snsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.snsService = snsService;
    }

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, null);
    }

    public UserResponse createUser(UserCreateRequest request){

        if (userRepository.existsByUsername(request.getUsername())){
            throw new IllegalArgumentException(
                    "A user with this username already exists."
            );
        }

        String token = UUID.randomUUID().toString();

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setVerificationToken(token);
        user.setTokenExpiry(OffsetDateTime.now().plusMinutes(1));
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        if (snsService != null) {
            snsService.publishUserSignup(savedUser, token);
        }

        return new UserResponse(savedUser);
    }

    public void verifyEmail(String email, String token) {
        if (email == null || email.isBlank() || token == null || token.isBlank()) {
            throw new IllegalArgumentException("Email and verification token are required.");
        }

        User user = userRepository.findByUsername(email)
                .orElseThrow(() -> new IllegalArgumentException("User with this email does not exist."));

        if (user.getVerificationToken() == null || !user.getVerificationToken().equals(token)) {
            throw new IllegalArgumentException("Invalid verification token.");
        }

        if (user.getTokenExpiry() == null || OffsetDateTime.now().isAfter(user.getTokenExpiry())) {
            throw new IllegalArgumentException("Verification link has expired. Links are valid for 1 minute.");
        }

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(OffsetDateTime.now());
        userRepository.save(user);
    }

    public UserResponse getuser(
            Long userId,
            String authenticatedUsername
    ){
        User authenticatedUser = userRepository.findByUsername(authenticatedUsername)
                .orElseThrow(() ->
                        new IllegalArgumentException("Authenticated user not found.")
                );

        if(!authenticatedUser.getId().equals(userId)){
            throw new SecurityException(
                    "You may only access your own account"
            );
        }

        return new UserResponse(authenticatedUser);
    }

    public void updateUser(
            Long userId,
            String authenticatedUsername,
            UserUpdateRequest request
    ){
        User authenticatedUser = userRepository.findByUsername(authenticatedUsername)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Authenticated user not found."
                        )
                );

        if(!authenticatedUser.getId().equals(userId)){
            throw new SecurityException(
                    "You may only update your own account."
            );
        }

        if (request.hasUnsupportedFields()){
            throw new IllegalArgumentException(
                    "Only first_name, last_name, and password can be updated."
            );
        }

        boolean hasUpdate = false;

        if(request.getFirstName() != null){
            if(request.getFirstName().isBlank()){
                throw new IllegalArgumentException(
                        "First name cannot be blank"
                        );
            }

            authenticatedUser.setFirstName(request.getFirstName());
            hasUpdate = true;
        }

        if(request.getLastName() != null){
            if (request.getLastName().isBlank()){
                throw new IllegalArgumentException(
                        "Last name cannot be blank."
                );
            }
            authenticatedUser.setLastName(request.getLastName());
            hasUpdate = true;
        }
        if(request.getPassword() != null){
            if (request.getPassword().isBlank()){
                throw new IllegalArgumentException(
                        "Password cannot be blank."
                );
            }
            authenticatedUser.setPassword(passwordEncoder.encode(
                    request.getPassword()
            ));
            hasUpdate = true;
        }

        if (!hasUpdate){
            throw new IllegalArgumentException(
                    "At least one updateable field must be provided"
            );
        }
    userRepository.save(authenticatedUser);
    }
}
