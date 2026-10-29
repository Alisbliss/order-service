package com.innowise.orderservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    //404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException e, HttpServletRequest r) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), r.getRequestURI());
    }

    //422-business(empty order, quantity <= 0)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest r) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage(), r.getRequestURI());
    }

    //503-user-service unavailable(CircuitBreaker)
    @ExceptionHandler(UserServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUserServiceUnavailable(UserServiceUnavailableException e,
                                                                      HttpServletRequest r) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), r.getRequestURI());
    }

    //400-validation DTO (@NotBlank, @NotNull, @Min)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e,
                                                          HttpServletRequest r) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, r.getRequestURI());
    }

    //403-@PreAuthorize
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(RuntimeException e, HttpServletRequest r) {
        return build(HttpStatus.FORBIDDEN, "Forbidden: Access Denied", r.getRequestURI());
    }

    //500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e, HttpServletRequest r) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", r.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                LocalDateTime.now(ZoneOffset.UTC),
                status.value(),
                status.getReasonPhrase(),
                message,
                path));
    }
}
