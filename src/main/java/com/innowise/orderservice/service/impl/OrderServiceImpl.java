package com.innowise.orderservice.service.impl;

import com.innowise.orderservice.client.UserServiceClient;
import com.innowise.orderservice.dto.*;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.exception.BusinessException;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.mapper.OrderItemMapper;
import com.innowise.orderservice.mapper.OrderMapper;
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

@Service("orderService")
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    public OrderResponse create(OrderCreateRequest request) {
        UserInfoResponse user = userServiceClient.getByEmail(request.getUserEmail());
        Order order = orderMapper.toEntity(request);
        order.setUserId(user.getId());
        order.setUserEmail(request.getUserEmail());
        order.setStatus(OrderStatus.CREATED);
        order.setDeleted(false);
        order.setTotalPrice(recalculateTotal(order));

        Order saved = orderRepository.save(order);
        return toResponse(saved, user);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("Order not found: " + id));
        UserInfoResponse user = userServiceClient.getByEmail(order.getUserEmail());
        return toResponse(order, user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAll(LocalDateTime from, LocalDateTime to,
                              Collection<OrderStatus> statuses, Pageable pageable) {
        return orderRepository.findAll(OrderSpecification.filter(from, to, statuses), pageable)
                .map(order -> toResponse(order, userServiceClient.getByEmail(order.getUserEmail())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getByUserId(Long userId) {
        return orderRepository.findAllByUserId(userId).stream()
                .map(order -> toResponse(order, userServiceClient.getByEmail(order.getUserEmail())))
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse update(Long id, OrderUpdateRequest updateRequest) {
        Order order = orderRepository.findById(id).orElseThrow(()
                -> new ResourceNotFoundException("Order not found: " + id));
        if (updateRequest.getStatus() != null) order.setStatus(updateRequest.getStatus());
        if (updateRequest.getItems() != null) {
            if (updateRequest.getItems().isEmpty()) {
                throw new BusinessException("Order must contain at least one item");
            }
            order.getItems().clear();
            for (OrderItemCreateRequest itemReq : updateRequest.getItems()) {
                OrderItem orderItem = orderItemMapper.toEntity(itemReq);
                order.getItems().add(orderItem);
            }
            order.setTotalPrice(recalculateTotal(order));
        }

        Order saved = orderRepository.save(order);
        UserInfoResponse user = userServiceClient.getByEmail(saved.getUserEmail());
        return toResponse(saved, user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Order order = orderRepository.findById(id).orElseThrow(()
                        -> new ResourceNotFoundException("Order not found: " + id));
        orderRepository.delete(order);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCurrentUser(String userEmail, Long userId) {
        if (userEmail == null || userId == null) return false;
        try {
            UserInfoResponse user = userServiceClient.getByEmail(userEmail);
            return user.getId().equals(userId);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOrderOwner(Long orderId, Long userId) {
        if (orderId == null || userId == null) return false;
        return orderRepository.findById(orderId)
                .map(order -> order.getUserId().equals(userId))
                .orElse(false);
    }

    private BigDecimal recalculateTotal(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem orderItem : order.getItems()) {
            if (orderItem.getItem() == null || orderItem.getItem().getId() == null) {
                throw new BusinessException("Order item must reference an existing item");
            }
            if (orderItem.getQuantity() == null || orderItem.getQuantity() <= 0) {
                throw new BusinessException("Quantity must be positive");
            }
            Item item = itemRepository.findById(orderItem.getItem().getId()).orElseThrow(()
                    -> new ResourceNotFoundException("Item not found: " + orderItem.getItem().getId()));
            orderItem.setItem(item);
            orderItem.setOrder(order);
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        }
        return total;
    }

    private OrderResponse toResponse(Order order, UserInfoResponse user) {
        OrderResponse response = orderMapper.toResponse(order);
        response.setUser(user);
        return response;
    }
}
