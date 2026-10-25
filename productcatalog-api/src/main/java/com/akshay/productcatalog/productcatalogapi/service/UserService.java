package com.akshay.productcatalog.productcatalogapi.service;

import com.akshay.productcatalog.productcatalogapi.dto.UserCreateRequest;
import com.akshay.productcatalog.productcatalogapi.dto.UserResponse;
import com.akshay.productcatalog.productcatalogapi.dto.UserUpdateRequest;
import com.akshay.productcatalog.productcatalogapi.entity.User;
import com.akshay.productcatalog.productcatalogapi.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse createUser(UserCreateRequest request){

        if (userRepository.existsByUsername(request.getUsername())){
            throw new IllegalArgumentException(
                    "A user with this username already exists."
            );
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        return new UserResponse(savedUser);
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
