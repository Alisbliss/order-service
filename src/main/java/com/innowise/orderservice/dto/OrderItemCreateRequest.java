package com.innowise.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemCreateRequest {
    @NotNull(message = "itemId is required")
    private Long itemId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be >= 1")
    private Integer quantity;
}
