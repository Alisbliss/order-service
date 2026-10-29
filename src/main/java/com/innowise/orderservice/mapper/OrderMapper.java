package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.OrderCreateRequest;
import com.innowise.orderservice.dto.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.innowise.orderservice.entity.Order;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {OrderItemMapper.class})
public interface OrderMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "userEmail", source = "userEmail")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "items", source = "items")
    Order toEntity(OrderCreateRequest request);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "items", source = "items")
    OrderResponse toResponse(Order order);
}
