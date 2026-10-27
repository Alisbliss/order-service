package com.innowise.orderservice.service;

import com.innowise.orderservice.dto.OrderCreateRequest;
import com.innowise.orderservice.dto.OrderResponse;
import com.innowise.orderservice.dto.OrderUpdateRequest;
import com.innowise.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OrderService {
    OrderResponse create(OrderCreateRequest request);
    OrderResponse getById(Long id);
    Page<OrderResponse> getAll(LocalDateTime createdFrom, LocalDateTime createdTo, Collection<OrderStatus> statuses, Pageable pageable);
    List<OrderResponse> getByUserId(Long userId);
    OrderResponse update(Long id, OrderUpdateRequest request);
    void delete(Long id);
}
