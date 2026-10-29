package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.OrderItemCreateRequest;
import com.innowise.orderservice.dto.OrderItemResponse;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderItemMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "item", source = "itemId", qualifiedByName = "longToItem")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderItem toEntity(OrderItemCreateRequest request);

    @Named("longToItem")
    default Item longToItem(Long itemId) {
        if (itemId == null) return null;
        Item item = new Item();
        item.setId(itemId);
        return item;
    }

    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "itemName", source = "item.name")
    OrderItemResponse toResponse(OrderItem orderItem);
}
