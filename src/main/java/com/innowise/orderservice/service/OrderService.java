package com.innowise.orderservice.service;

import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OrderService {
    Order create(Order order);
    Order getById(Long id);
    Page<Order> getAll(LocalDateTime createdFrom, LocalDateTime createdTo, Collection<OrderStatus> statuses, Pageable pageable);
    List<Order> getByUserId(Long userId);
    Order update(Long id, Order patch);
    void delete(Long id);
}
