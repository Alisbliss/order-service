package com.innowise.orderservice.controller;

import com.innowise.orderservice.dto.OrderCreateRequest;
import com.innowise.orderservice.dto.OrderResponse;
import com.innowise.orderservice.dto.OrderUpdateRequest;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or @orderService.isCurrentUser(#request.userEmail, authentication.principal)")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @orderService.isOrderOwner(#id, authentication.principal)")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> getAll(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Collection<OrderStatus> statuses,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(orderService.getAll(from, to, statuses, pageable));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    public ResponseEntity<List<OrderResponse>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getByUserId(userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @orderService.isOrderOwner(#id, authentication.principal)")
    public ResponseEntity<OrderResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody OrderUpdateRequest request) {
        return ResponseEntity.ok(orderService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @orderService.isOrderOwner(#id, authentication.principal)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
