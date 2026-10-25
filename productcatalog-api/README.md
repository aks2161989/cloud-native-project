# Product Catalog API

## Overview

Product Catalog API is a cloud-native RESTful backend application developed using **Spring Boot** and **PostgreSQL**.

The application provides:

- User registration
- Basic Authentication
- Secure password storage using BCrypt
- User profile retrieval and updates
- Product CRUD operations
- Product ownership authorization
- Health monitoring endpoint
- Automatic database schema generation using Hibernate
- Integration testing using Spring Boot and MockMvc

The application follows a layered architecture consisting of Controllers, Services, Repositories, DTOs, and JPA Entities.

---

## Technology Stack

- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate ORM
- PostgreSQL
- Maven
- Jakarta Bean Validation
- JUnit 5
- Spring Boot Test
- MockMvc

---

## Features

- RESTful API design
- User registration
- HTTP Basic Authentication
- BCrypt password hashing
- User profile management
- Product creation, retrieval, update, and deletion
- Owner-based authorization for product modification
- Request validation
- Global exception handling
- Automatic schema creation and updates
- Health check endpoint
- Integration tests

---

# API Endpoints

| Method | Endpoint | Authentication |
|---------|----------|----------------|
| GET | `/healthz` | No |
| POST | `/v1/user` | No |
| GET | `/v1/user/self` | Yes |
| PUT | `/v1/user/self` | Yes |
| POST | `/v1/product` | Yes |
| GET | `/v1/product/{id}` | Yes |
| PUT | `/v1/product/{id}` | Yes |
| DELETE | `/v1/product/{id}` | Yes |

---

# Authentication

All protected endpoints use **HTTP Basic Authentication**.

Example:

```bash
curl -u test@example.com:Password123 http://localhost:8080/v1/user/self
```

---

# Database Setup

Create the PostgreSQL database:

```sql
CREATE DATABASE productcatalog_db;
```

---

# Configuration

Update the following file:

```text
src/main/resources/application.properties
```

Example configuration:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/productcatalog_db
spring.datasource.username=postgres
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

spring.jpa.open-in-view=false
server.error.include-stacktrace=never
```

---

# Running the Application

Start the PostgreSQL service.

Run the application:

```bash
mvn spring-boot:run
```

or run the application directly from IntelliJ IDEA.

---

# Running Tests

Execute all integration tests:

```bash
mvn test
```

The project includes integration tests built using:

- SpringBootTest
- MockMvc
- Spring Security Test

---

# Example API Requests

## Health Check

```bash
curl -i http://localhost:8080/healthz
```

---

## Create User

```bash
curl -i -X POST http://localhost:8080/v1/user \
-H "Content-Type: application/json" \
-d '{
  "first_name":"John",
  "last_name":"Doe",
  "username":"john@example.com",
  "password":"Password123"
}'
```

---

## Retrieve Authenticated User

```bash
curl -i -u john@example.com:Password123 \
http://localhost:8080/v1/user/self
```

---

## Create Product

```bash
curl -i -X POST http://localhost:8080/v1/product \
-u john@example.com:Password123 \
-H "Content-Type: application/json" \
-d '{
  "name":"Keyboard",
  "description":"Mechanical keyboard",
  "sku":"KB-001",
  "manufacturer":"KeyCorp",
  "quantity":10
}'
```

---

## Health Endpoint Responses

| Scenario | Status |
|----------|--------|
| Successful request | 200 OK |
| Request contains payload | 400 Bad Request |
| Unsupported HTTP method | 405 Method Not Allowed |
| Database unavailable | 503 Service Unavailable |

---

# Automatic Schema Management

Hibernate automatically creates and updates the database schema during application startup.

No manual SQL scripts are required.

---

# Project Structure

```text
src
├── main
│   ├── java
│   │   └── com.akshay.productcatalog.productcatalogapi
│   │       ├── config
│   │       ├── controller
│   │       ├── dto
│   │       ├── entity
│   │       ├── exception
│   │       ├── repository
│   │       ├── security
│   │       └── service
│   └── resources
│       └── application.properties
│
└── test
    └── java
        └── com.akshay.productcatalog.productcatalogapi
            ├── ProductcatalogApiApplicationTests
            ├── UserIntegrationTest
            └── ProductIntegrationTest
```

---

# Build

Build the application:

```bash
mvn clean package
```

Run the generated JAR:

```bash
java -jar target/productcatalog-api-0.0.1-SNAPSHOT.jar
```

## Testing

The project includes automated tests using:

- JUnit 5
- Spring MockMvc
- REST Assured
- Spring Boot integration testing
- PostgreSQL-backed end-to-end testing

The test suite covers:

- Health endpoint behavior
- User registration and validation
- Basic authentication
- User updates
- Product CRUD operations
- Public product retrieval
- Owner-based authorization
- Invalid request handling
- Missing-resource handling

Run the complete test suite with:

```bash
mvn clean test