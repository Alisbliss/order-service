package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.exception.BusinessException;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.repository.specification.OrderSpecification;
import com.innowise.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public Order create(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BusinessException("Order must contain at least one item");
        }
        order.setStatus(OrderStatus.CREATED);
        order.setDeleted(false);
        order.setTotalPrice(recalculateTotal(order));
        return orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Order getById(Long id) {
        return orderRepository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("Order not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Order> getAll(LocalDateTime from, LocalDateTime to,
                              Collection<OrderStatus> statuses, Pageable pageable) {
        return orderRepository.findAll(OrderSpecification.filter(from, to, statuses), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getByUserId(Long userId) {
        return orderRepository.findAllByUserId(userId);
    }

    @Override
    @Transactional
    public Order update(Long id, Order update) {
        Order order = getById(id);
        if (update.getStatus() != null) order.setStatus(update.getStatus());
        if (update.getUserId() != null) order.setUserId(update.getUserId());
        if (update.getItems() != null && !update.getItems().isEmpty()) {
            order.getItems().clear();
            order.getItems().addAll(update.getItems());
            order.setTotalPrice(recalculateTotal(order));
        }
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Order order = getById(id);
        orderRepository.delete(order);
    }

    private BigDecimal recalculateTotal(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem orderItem : order.getItems()) {
            Item item = itemRepository.findById(orderItem.getItem().getId()).orElseThrow(()
                    -> new ResourceNotFoundException("Item not found: " + orderItem.getItem().getId()));
            orderItem.setItem(item);
            orderItem.setOrder(order);
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        }
        return total;
    }
}
