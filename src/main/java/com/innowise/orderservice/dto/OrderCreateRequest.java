package com.innowise.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {
    @NotBlank(message = "userEmail is required")
    private String userEmail;

    @NotNull(message = "items are required")
    @NotEmpty(message = "order must contain at least one item")
    @Valid
    private List<OrderItemCreateRequest> items;
}
