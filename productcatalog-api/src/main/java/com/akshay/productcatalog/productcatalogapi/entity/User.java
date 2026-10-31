package com.akshay.productcatalog.productcatalogapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
public class User {

    public Long getId() {
        return id;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotBlank
    @Email
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "account_created", nullable = false, updatable = false)
    private OffsetDateTime accountCreated;

    @Column(name = "account_updated", nullable = false)
    private OffsetDateTime accountUpdated;

    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerified = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "token_expiry")
    private OffsetDateTime tokenExpiry;

    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

    @PrePersist
    protected void onCreate(){
        OffsetDateTime now = OffsetDateTime.now();
        this.accountCreated = now;
        this.accountUpdated = now;
    }

    @PreUpdate
    protected void onUpdate(){
        this.accountUpdated = OffsetDateTime.now();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public OffsetDateTime getAccountCreated() {
        return accountCreated;
    }

    public OffsetDateTime getAccountUpdated() {
        return accountUpdated;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public OffsetDateTime getTokenExpiry() {
        return tokenExpiry;
    }

    public void setTokenExpiry(OffsetDateTime tokenExpiry) {
        this.tokenExpiry = tokenExpiry;
    }

    public OffsetDateTime getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(OffsetDateTime emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }
}
