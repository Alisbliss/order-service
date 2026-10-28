package com.innowise.orderservice.service;

import com.innowise.orderservice.client.UserServiceClient;
import com.innowise.orderservice.dto.*;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderItem;
import com.innowise.orderservice.entity.OrderStatus;
import com.innowise.orderservice.exception.BusinessException;
import com.innowise.orderservice.exception.ResourceNotFoundException;
import com.innowise.orderservice.exception.UserServiceUnavailableException;
import com.innowise.orderservice.mapper.OrderItemMapper;
import com.innowise.orderservice.mapper.OrderMapper;
import com.innowise.orderservice.repository.ItemRepository;
import com.innowise.orderservice.repository.OrderRepository;
import com.innowise.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private UserServiceClient userServiceClient;
    @InjectMocks private OrderServiceImpl orderService;

    private OrderCreateRequest createRequest() {
        return OrderCreateRequest.builder()
                .userEmail("user@example.com")
                .items(List.of(
                        OrderItemCreateRequest.builder().itemId(1L).quantity(2).build()
                ))
                .build();
    }

    private UserInfoResponse userInfo() {
        return UserInfoResponse.builder()
                .id(5L).name("Alex").surname("Aleksandrov")
                .birthDate(LocalDate.of(1989, 4, 8))
                .email("user@example.com").active(true)
                .build();
    }

    private Item item(Long id, String price) {
        Item i = new Item();
        i.setId(id);
        i.setName("Mouse");
        i.setPrice(new BigDecimal(price));
        return i;
    }

    private Order orderWithItems(Long id, Long userId) {
        Order o = new Order();
        o.setId(id);
        o.setUserId(userId);
        o.setStatus(OrderStatus.CREATED);
        o.setUserEmail("user@example.com");
        o.setDeleted(false);
        OrderItem oi = new OrderItem();
        oi.setId(10L);
        oi.setItem(item(1L, "20.02"));
        oi.setQuantity(2);
        oi.setOrder(o);
        o.setItems(new ArrayList<>(List.of(oi)));
        return o;
    }

    //create
    @Test
    void create_shouldCallUserServiceAndSaveOrder() {
        OrderCreateRequest req = createRequest();
        UserInfoResponse user = userInfo();
        Order mapped = new Order();
        OrderItem oi = new OrderItem();
        oi.setItem(item(1L, "0"));
        oi.setQuantity(2);
        mapped.setItems(new ArrayList<>(List.of(oi)));

        when(userServiceClient.getByEmail("user@example.com")).thenReturn(user);
        when(orderMapper.toEntity(req)).thenReturn(mapped);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item(1L, "20.02")));
        when(orderRepository.save(mapped)).thenReturn(mapped);
        OrderResponse response = new OrderResponse();
        when(orderMapper.toResponse(mapped)).thenReturn(response);

        OrderResponse result = orderService.create(req);

        assertThat(result).isSameAs(response);
        assertThat(mapped.getUserId()).isEqualTo(5L);
        assertThat(mapped.getUserEmail()).isEqualTo("user@example.com");
        assertThat(mapped.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(mapped.getTotalPrice()).isEqualByComparingTo("40.04");
        verify(orderRepository).save(mapped);
    }

    @Test
    void create_shouldThrowWhenItemsEmpty() {
        OrderCreateRequest req = createRequest();
        req.setItems(List.of());
        Order mapped = new Order();
        mapped.setItems(new ArrayList<>());

        when(userServiceClient.getByEmail("user@example.com")).thenReturn(userInfo());
        when(orderMapper.toEntity(req)).thenReturn(mapped);

        assertThatThrownBy(() -> orderService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("at least one item");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowWhenItemNotFound() {
        OrderCreateRequest req = createRequest();
        Order mapped = new Order();
        OrderItem oi = new OrderItem();
        oi.setItem(item(99L, "0"));
        oi.setQuantity(1);
        mapped.setItems(new ArrayList<>(List.of(oi)));

        when(userServiceClient.getByEmail("user@example.com")).thenReturn(userInfo());
        when(orderMapper.toEntity(req)).thenReturn(mapped);
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Item not found: 99");
    }

    @Test
    void create_shouldThrowWhenQuantityInvalid() {
        OrderCreateRequest req = createRequest();
        Order mapped = new Order();
        OrderItem oi = new OrderItem();
        oi.setItem(item(1L, "0"));
        oi.setQuantity(0);
        mapped.setItems(new ArrayList<>(List.of(oi)));

        when(userServiceClient.getByEmail("user@example.com")).thenReturn(userInfo());
        when(orderMapper.toEntity(req)).thenReturn(mapped);

        assertThatThrownBy(() -> orderService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quantity must be positive");
    }

    //getById
    @Test
    void getById_shouldReturnOrderWithUser() {
        Order order = orderWithItems(1L, 5L);
        UserInfoResponse user = userInfo();
        OrderResponse response = new OrderResponse();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userServiceClient.getByEmail("user@example.com")).thenReturn(user);
        when(orderMapper.toResponse(order)).thenReturn(response);
        OrderResponse result = orderService.getById(1L);

        assertThat(result).isSameAs(response);
        verify(userServiceClient).getByEmail("user@example.com");
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found: 99");
    }

    //getAll
    @Test
    void getAll_shouldReturnPageWithUserInfo() {
        Pageable pageable = PageRequest.of(0, 10);
        Order order = orderWithItems(1L, 5L);
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);
        UserInfoResponse user = userInfo();
        OrderResponse response = new OrderResponse();

        when(orderRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(page);
        when(userServiceClient.getByEmail("user@example.com")).thenReturn(user);
        when(orderMapper.toResponse(order)).thenReturn(response);

        Page<OrderResponse> result = orderService.getAll(null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0)).isSameAs(response);
    }

    //getByUserId
    @Test
    void getByUserId_shouldReturnListWithUserInfo() {
        Order order = orderWithItems(1L, 5L);
        UserInfoResponse user = userInfo();
        OrderResponse response = new OrderResponse();

        when(orderRepository.findAllByUserId(5L)).thenReturn(List.of(order));
        when(userServiceClient.getByEmail("user@example.com")).thenReturn(user);
        when(orderMapper.toResponse(order)).thenReturn(response);
        List<OrderResponse> result = orderService.getByUserId(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(response);
    }

    //update
    @Test
    void update_shouldUpdateStatusAndRecalculateTotal() {
        Order order = orderWithItems(1L, 5L);
        OrderUpdateRequest req = OrderUpdateRequest.builder()
                .status(OrderStatus.PAID)
                .items(List.of(OrderItemCreateRequest.builder().itemId(1L).quantity(3).build()))
                .build();

        OrderItem newOi = new OrderItem();
        newOi.setItem(item(1L, "0"));
        newOi.setQuantity(3);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemMapper.toEntity(any(OrderItemCreateRequest.class))).thenReturn(newOi);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item(1L, "20.02")));
        when(orderRepository.save(order)).thenReturn(order);
        when(userServiceClient.getByEmail("user@example.com")).thenReturn(userInfo());
        when(orderMapper.toResponse(order)).thenReturn(new OrderResponse());
        orderService.update(1L, req);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("60.06");
        verify(orderRepository).save(order);
    }

    @Test
    void update_shouldThrowWhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.update(99L, new OrderUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_shouldThrowWhenItemsEmpty() {
        Order order = orderWithItems(1L, 5L);
        OrderUpdateRequest req = OrderUpdateRequest.builder().items(List.of()).build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.update(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("at least one item");
        verify(orderRepository, never()).save(any());
    }

    //delete
    @Test
    void delete_shouldCallRepositoryDelete() {
        Order order = orderWithItems(1L, 5L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        orderService.delete(1L);

        verify(orderRepository).delete(order);
    }

    @Test
    void delete_shouldThrowWhenNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(orderRepository, never()).delete(any(Order.class));
    }

    //circuit breaker fallback
    @Test
    void create_shouldPropagateUserServiceUnavailable() {
        OrderCreateRequest req = createRequest();
        when(userServiceClient.getByEmail("user@example.com"))
                .thenThrow(new UserServiceUnavailableException("down"));

        assertThatThrownBy(() -> orderService.create(req))
                .isInstanceOf(UserServiceUnavailableException.class);
        verify(orderRepository, never()).save(any());
    }
}