package com.innowise.orderservice.repository.specification;

import com.innowise.orderservice.entity.Order;
import com.innowise.orderservice.entity.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import jakarta.persistence.criteria.Predicate;

public final class OrderSpecification {
    private OrderSpecification() {}

    public static Specification<Order> filter(LocalDateTime from,
                                              LocalDateTime to,
                                              Collection<OrderStatus> statuses) {
        return (root, query, cb) -> {
            ArrayList<Predicate> predicates = new ArrayList<Predicate>();
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            if (statuses != null && !statuses.isEmpty()) predicates.add(root.get("status").in(statuses));
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}