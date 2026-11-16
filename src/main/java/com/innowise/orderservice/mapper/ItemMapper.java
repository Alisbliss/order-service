package com.innowise.orderservice.mapper;

import com.innowise.orderservice.dto.ItemResponse;
import com.innowise.orderservice.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ItemMapper {
    ItemResponse toResponse(Item item);
}
