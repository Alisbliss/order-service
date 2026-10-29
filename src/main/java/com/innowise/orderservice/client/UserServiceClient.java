package com.innowise.orderservice.client;

import com.innowise.orderservice.dto.UserInfoResponse;
import com.innowise.orderservice.exception.UserServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserServiceClient {
    private static final Logger log = LoggerFactory.getLogger(UserServiceClient.class);
    private final UserClient userClient;

    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackGetByEmail")
    public UserInfoResponse getByEmail(String email) {
        return userClient.getByEmail(email);
    }

    private UserInfoResponse fallbackGetByEmail(String email, Throwable t) {
        log.warn("user-service unavailable for email={}: {}", email, t.getMessage());
        throw new UserServiceUnavailableException(
                "User service is unavailable, cannot fetch user by email: " + email);
    }
}
