package com.innowise.orderservice.dto;

import com.innowise.orderservice.entity.OrderStatus;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpdateRequest {
    private OrderStatus status;

    @Valid
    private List<OrderItemCreateRequest> items;
}
