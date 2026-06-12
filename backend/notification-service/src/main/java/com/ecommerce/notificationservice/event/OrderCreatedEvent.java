package com.ecommerce.notificationservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents the event payload published when a new order is created.
 * Requirement: 1.2
 */
public record OrderCreatedEvent(
        String orderId,
        String userId,
        String userEmail,
        BigDecimal totalAmount,
        List<OrderItemEvent> items,
        LocalDateTime createdAt
) {
}
