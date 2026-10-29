# order-service

Microservice for managing orders and order items.

## Tech Stack

- Java 21, Spring Boot 4
- PostgreSQL, Liquibase
- MapStruct, Lombok
- Spring Data JPA (Specifications, JPA Auditing)
- OpenFeign, Resilience4j (CircuitBreaker)
- Spring Security, JWT
- JUnit 5, Mockito, Testcontainers, WireMock
- Docker, GitHub Actions

## Features

- CRUD for Order and OrderItem
- Soft delete with cascading operations
- Pagination and filtering by creation date range and statuses
- Feign call to `user-service` with CircuitBreaker
- Order responses include order + user info
- JWT authorization: USER - own orders, ADMIN - all
- Global exception handling
- Unit + integration tests
- CI with build, test and Docker image

## Run with Docker

docker compose up --build