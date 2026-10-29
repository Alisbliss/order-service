package com.innowise.orderservice.dto;

import com.innowise.orderservice.entity.OrderStatus;
import jakarta.validation.Valid;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderUpdateRequest {
    private OrderStatus status;

    @Valid
    private List<OrderItemCreateRequest> items;
}
